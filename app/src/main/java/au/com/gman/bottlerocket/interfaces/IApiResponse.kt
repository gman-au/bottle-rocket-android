package au.com.gman.bottlerocket.interfaces

interface IApiResponse {
    val errorCode: Int
    val errorMessage: String
    fun isSuccess(): Boolean = errorCode == 0
}