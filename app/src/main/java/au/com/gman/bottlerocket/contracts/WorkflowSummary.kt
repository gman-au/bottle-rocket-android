package au.com.gman.bottlerocket.contracts

import com.google.gson.annotations.SerializedName

data class WorkflowSummary(
    @SerializedName("id")
    val workflowId: String,

    @SerializedName("name")
    val workflowName: String
)