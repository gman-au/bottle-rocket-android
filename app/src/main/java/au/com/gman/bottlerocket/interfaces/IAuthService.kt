package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.contracts.LoginResponse

interface IAuthService {

    fun signIn(baseUrl: String, username: String, password: String): LoginResponse?

    fun loginAgainst(baseUrl: String, username: String, password: String): String?

    fun logoutAgainst(baseUrl: String, accessToken: String)
}