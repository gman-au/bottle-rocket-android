package au.com.gman.bottlerocket.network

import android.os.Build
import android.util.Base64
import au.com.gman.bottlerocket.contracts.LoginResponse
import au.com.gman.bottlerocket.interfaces.IAuthService
import au.com.gman.bottlerocket.settings.AppSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    @Named("auth") private val client: OkHttpClient,
    private val appSettings: AppSettings,
    private val tokenStore: TokenStore,
    private val sessionEvents: SessionEvents
) : IAuthService {
    private val json = "application/json".toMediaType()

    override fun loginAgainst(baseUrl: String, username: String, password: String): String? {
        val body = JSONObject()
            .put("user_name", username)
            .put("password", password)
            .put("device_name", "${Build.MANUFACTURER} ${Build.MODEL} (connection test)")
            .put("use_cookie", false)

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/auth/login")
            .post(body.toString().toRequestBody(json))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return JSONObject(response.body!!.string()).getString("access_token")
        }
    }

    override fun signIn(baseUrl: String, username: String, password: String): LoginResponse? {

        val body =
            JSONObject()
                .put("user_name", username)
                .put("password", password)
                .put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
                .put("use_cookie", false)

        val request =
            Request
                .Builder()
                .url(baseUrl.trimEnd('/') + "/api/auth/login")
                .post(body.toString().toRequestBody(json))
                .build()

        client
            .newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) return null
                val result = JSONObject(response.body!!.string())

                tokenStore
                    .save(result.getString("access_token"), result.getString("refresh_token"))

                appSettings.apiBaseUrl = baseUrl
                appSettings.username = username

                return LoginResponse(
                    userName = result.optString("user_name"),
                    role = result.optString("role"),
                    darkMode = result.optBoolean("dark_mode"),
                    currentLanguage = result.optString("current_language")
                )
            }
    }

    /**
     * Returns a usable access token, or null if one couldn't be obtained.
     * [staleAccessToken] is the token the caller was using. If another thread has
     * already replaced it, we return the newer one instead of refreshing again.
     * Refreshing twice with the same refresh token would trip the server's replay detection.
     */
    @Synchronized
    fun refreshAccessToken(staleAccessToken: String?): String? {
        val current = tokenStore.accessToken
        if (current != null && current != staleAccessToken) return current

        val refresh = tokenStore.refreshToken ?: return null

        val request =
            Request
                .Builder()
                .url(url("/api/auth/refresh"))
                .post(JSONObject().put("refresh_token", refresh).toString().toRequestBody(json))
                .build()

        return try {
            client
                .newCall(request)
                .execute()
                .use { response ->
                    when {
                        response.isSuccessful -> {
                            val result = JSONObject(response.body!!.string())
                            val access = result.getString("access_token")
                            tokenStore.save(access, result.getString("refresh_token"))
                            access
                        }

                        response.code == 401 -> {
                            // Session expired or revoked: a real logout
                            tokenStore.clear()
                            sessionEvents.notifyLoggedOut()
                            null
                        }

                        else -> null // 5xx etc.: keep the session and retry later
                    }
                }
        } catch (e: IOException) {
            null // offline or timeout: not a logout
        }
    }

    /** Revokes the session server-side, then clears local state regardless of the outcome. */
    fun logout() {
        try {
            val access = tokenStore.accessToken
                ?.let { if (isExpiring(it)) refreshAccessToken(it) ?: it else it }

            if (access != null) {
                val request =
                    Request
                        .Builder()
                        .url(url("/api/auth/logout"))
                        .header("Authorization", "Bearer $access")
                        .post("".toRequestBody(json))
                        .build()
                client
                    .newCall(request)
                    .execute()
                    .close()
            }
        } catch (_: IOException) {
            // Server-side revoke failed. The session lives until it expires, but we still forget it locally.
        } finally {
            tokenStore
                .clear()
        }
    }

    /** Reads `exp` from the JWT payload, so no date-format parsing is needed. */
    fun isExpiring(token: String, skewSeconds: Long = 60): Boolean {
        val exp = try {
            val payload = token.split(".")[1]
            val decoded =
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            JSONObject(String(decoded)).getLong("exp")
        } catch (_: Exception) {
            return false // can't tell, so let the server decide (401 path)
        }
        return exp - System.currentTimeMillis() / 1000 < skewSeconds
    }

    private fun url(path: String) =
        appSettings
            .apiBaseUrl
            .trimEnd('/') + path
}