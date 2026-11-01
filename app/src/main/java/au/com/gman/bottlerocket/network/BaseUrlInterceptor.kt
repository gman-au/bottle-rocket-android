package au.com.gman.bottlerocket.network

import au.com.gman.bottlerocket.settings.AppSettings
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Points every request at the currently saved server, so a settings change needs no restart. */
class BaseUrlInterceptor @Inject constructor(
    private val appSettings: AppSettings
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val target = appSettings.apiBaseUrl.toHttpUrlOrNull() ?: return chain.proceed(request)

        // Start from the saved base (keeps any reverse-proxy path prefix), then append the request's own path and query
        val newUrl = target.newBuilder()
            .addEncodedPathSegments(request.url.encodedPath.trimStart('/'))
            .encodedQuery(request.url.encodedQuery)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}