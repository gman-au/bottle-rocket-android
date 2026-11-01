package au.com.gman.bottlerocket.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val authService: AuthService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        var token = tokenStore.accessToken

        // Refresh before sending, so the server normally never sees an expired token
        if (token != null && authService.isExpiring(token)) {
            token = authService.refreshAccessToken(token) ?: token
        }

        val request =
            if (token != null)
                original.newBuilder().header("Authorization", "Bearer $token").build()
            else
                original

        return chain.proceed(request)
    }
}