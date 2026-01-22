package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.contracts.ConnectionTestResponse
import au.com.gman.bottlerocket.contracts.FetchPageTemplatesResponse
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface IRetrofitApi {
    @Multipart
    @POST("/api/capture/process")
    suspend fun apiCaptureProcess(
        @Part image: MultipartBody.Part,
        @Part("qr_code") qrCode: RequestBody,
        @Part("qr_bounding_box") qrBoundingBox: RequestBody
    ): Response<ProcessCaptureResponse>

    @POST("/api/connection")
    suspend fun apiConnectionTest()
            : Response<ConnectionTestResponse>

    @GET("/api/pageTemplates")
    suspend fun apiFetchPageTemplates()
            : Response<FetchPageTemplatesResponse>
}