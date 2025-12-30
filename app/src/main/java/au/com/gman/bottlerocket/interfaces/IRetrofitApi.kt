package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface IRetrofitApi {
    @Multipart
    @POST("/api/capture/process")
    suspend fun apiCaptureProcess(
        @Part image: MultipartBody.Part
    ): Response<ProcessCaptureResponse>
}