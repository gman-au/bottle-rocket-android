package au.com.gman.bottlerocket.workflows

import au.com.gman.bottlerocket.contracts.WorkflowSummary
import au.com.gman.bottlerocket.interfaces.IRetrofitApi
import au.com.gman.bottlerocket.interfaces.IWorkflowCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowCache @Inject constructor(
    private val retrofitApi: IRetrofitApi
) : IWorkflowCache {
    private var templates: List<WorkflowSummary> = emptyList()
    private var isLoaded = false

    companion object {
        private const val TEMPLATE_LOAD_TIMEOUT_MS = 5000L // 5 seconds
    }

    override suspend fun loadWorkflows(): Result<List<WorkflowSummary>> {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(TEMPLATE_LOAD_TIMEOUT_MS) {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val emptyJsonBody = "{}".toRequestBody(mediaType)
                    val response = retrofitApi.apiFetchWorkflows(emptyJsonBody)

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.isSuccess()) {
                            templates = body.workflows.toList()
                            isLoaded = true
                            Result.success(templates)
                        } else {
                            Result.failure(Exception(body.errorMessage))
                        }
                    } else {
                        Result.failure(Exception("HTTP Error: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getWorkflows(): List<WorkflowSummary> = templates

    override fun isWorkflowsLoaded(): Boolean = isLoaded
}