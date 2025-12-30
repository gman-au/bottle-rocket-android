package au.com.gman.bottlerocket.contracts

import com.google.gson.annotations.SerializedName

open class ApiResponse(
    @SerializedName("error_code")
    open val errorCode: Int = 0,

    @SerializedName("error_message")
    open val errorMessage: String = ""
) {
    fun isSuccess(): Boolean = errorCode == 0
}