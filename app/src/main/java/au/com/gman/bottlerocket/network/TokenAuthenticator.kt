package au.com.gman.bottlerocket.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val authService: AuthService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once: give up rather than loop
        if (response.priorResponse != null) return null

        // Request had no bearer token (e.g. a public endpoint), so there's nothing to refresh
        val usedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?: return null

        val newToken = authService.refreshAccessToken(usedToken) ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}