package au.com.gman.bottlerocket.contracts

import com.google.gson.annotations.SerializedName

data class CloseBatchRequest(
    @SerializedName("id") val id: String,
    @SerializedName("workflows") val workflows: List<String>
)