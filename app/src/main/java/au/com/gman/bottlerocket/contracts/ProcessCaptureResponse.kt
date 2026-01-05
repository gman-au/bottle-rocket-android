package au.com.gman.bottlerocket.contracts

import au.com.gman.bottlerocket.interfaces.IApiResponse
import com.google.gson.annotations.SerializedName

data class ProcessCaptureResponse(
    @SerializedName("error_code")
    override val errorCode: Int,

    @SerializedName("error_message")
    override val errorMessage: String
) : IApiResponse