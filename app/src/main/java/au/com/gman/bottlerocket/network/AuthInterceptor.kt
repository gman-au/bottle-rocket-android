package au.com.gman.bottlerocket.network

import au.com.gman.bottlerocket.settings.AppSettings
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val appSettings: AppSettings
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestBuilder =
            if (appSettings.hasCredentials()) {
                val credentials =
                    Credentials
                        .basic(
                            appSettings.username,
                            appSettings.password
                        )

                originalRequest
                    .newBuilder()
                    .header("Authorization", credentials)
            } else {
                originalRequest
                    .newBuilder()
            }

        return chain.proceed(requestBuilder.build())
    }
}