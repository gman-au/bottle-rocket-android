package au.com.gman.bottlerocket.contracts

data class LoginResponse(
    val userName: String?,
    val role: String?,
    val darkMode: Boolean,
    val currentLanguage: String?
)