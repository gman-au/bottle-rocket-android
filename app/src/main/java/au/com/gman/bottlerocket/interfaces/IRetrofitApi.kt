package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.contracts.CloseBatchRequest
import au.com.gman.bottlerocket.contracts.CloseBatchResponse
import au.com.gman.bottlerocket.contracts.ConnectionTestResponse
import au.com.gman.bottlerocket.contracts.FetchPageTemplatesResponse
import au.com.gman.bottlerocket.contracts.FetchWorkflowsResponse
import au.com.gman.bottlerocket.contracts.OpenBatchResponse
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface IRetrofitApi {

    @GET("/api/batches/open")
    suspend fun apiBatchOpen(): Response<OpenBatchResponse>

    @Multipart
    @POST("/api/capture/process")
    suspend fun apiCaptureProcess(
        @Part image: MultipartBody.Part,
        @Part("vendor") vendor: RequestBody,
        @Part("batch_id") batchId: RequestBody
    ): Response<ProcessCaptureResponse>

    @POST("/api/batches/close")
    suspend fun apiBatchClose(@Body body: CloseBatchRequest): Response<CloseBatchResponse>

    @POST("/api/connection")
    suspend fun apiConnectionTest()
            : Response<ConnectionTestResponse>

    @GET("/api/pageTemplates")
    suspend fun apiFetchPageTemplates()
            : Response<FetchPageTemplatesResponse>

    @POST("/api/workflows/fetch")
    suspend fun apiFetchWorkflows(
        @Body body: RequestBody
    )
            : Response<FetchWorkflowsResponse>
}