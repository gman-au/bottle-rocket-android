package au.com.gman.bottlerocket.interfaces
import au.com.gman.bottlerocket.contracts.WorkflowSummary

interface IWorkflowCache {
    suspend fun loadWorkflows(): Result<List<WorkflowSummary>>

    fun getWorkflows(): List<WorkflowSummary>

    fun isWorkflowsLoaded(): Boolean
}