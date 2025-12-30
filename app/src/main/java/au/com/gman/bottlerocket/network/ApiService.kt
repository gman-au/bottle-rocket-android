package au.com.gman.bottlerocket.network

import android.content.ContentResolver
import android.net.Uri
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import au.com.gman.bottlerocket.interfaces.IFileIo
import au.com.gman.bottlerocket.interfaces.IRetrofitApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ApiService @Inject constructor(
    private val fileIo: IFileIo,
    private val retrofitApi: IRetrofitApi
) : IApiService {
    private var listener: IApiResponseListener? = null

    override fun setListener(listener: IApiResponseListener) {
        this.listener = listener
    }

    override fun uploadCapture(
        imageUri: Uri,
        cacheDir: File,
        contentResolver: ContentResolver
    ) {
        // Launch coroutine for async operation
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = fileIo.loadImage(
                    imageUri,
                    cacheDir,
                    contentResolver
                )

                val requestFile =
                    file
                        .asRequestBody(
                            "image/*".toMediaTypeOrNull()
                        )

                val body = MultipartBody.Part.createFormData(
                    "image",
                    file.name,
                    requestFile
                )

                // Make actual API call
                val httpResponse =
                    retrofitApi
                        .apiCaptureProcess(body)

                // Delete temp file
                file
                    .delete()

                // Switch to Main thread for listener callbacks
                withContext(Dispatchers.Main) {
                    if (httpResponse.isSuccessful && httpResponse.body() != null) {
                        val response = httpResponse.body()!!

                        if (response.isSuccess()) {
                            listener?.onApiProcessCaptureSuccess(response)
                        } else {
                            listener?.onApiResponseFailure(response)
                        }
                    } else {
                        // HTTP error - create error response
                        val errorResponse = ProcessCaptureResponse(
                            errorCode = httpResponse.code(),
                            errorMessage = "HTTP Error: ${httpResponse.code()} - ${httpResponse.message()}"
                        )
                        listener?.onApiResponseFailure(errorResponse)
                    }
                }

            } catch (e: Exception) {
                // Handle exceptions
                withContext(Dispatchers.Main) {
                    val errorResponse = ProcessCaptureResponse(
                        errorCode = -1,
                        errorMessage = "Exception: ${e.message}"
                    )
                    listener?.onApiResponseFailure(errorResponse)
                }
            }
        }
    }
}