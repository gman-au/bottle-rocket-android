package au.com.gman.bottlerocket.contracts

import com.google.gson.annotations.SerializedName

data class PageTemplateSummary(
    @SerializedName("qr_code")
    val qrCode: String,

    @SerializedName("book_vendor")
    val bookVendor: String
)