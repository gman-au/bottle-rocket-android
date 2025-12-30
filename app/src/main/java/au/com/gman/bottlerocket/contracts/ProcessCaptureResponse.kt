package au.com.gman.bottlerocket.contracts

import com.google.gson.annotations.SerializedName

data class ProcessCaptureResponse(
    @SerializedName("error_code")
    val errorCode: Int,

    @SerializedName("error_message")
    val errorMessage: String
) {
    fun isSuccess(): Boolean = errorCode == 0
}