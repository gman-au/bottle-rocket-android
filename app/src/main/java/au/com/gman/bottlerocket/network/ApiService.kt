package au.com.gman.bottlerocket.network

import au.com.gman.bottlerocket.contracts.ConnectionTestResponse
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import au.com.gman.bottlerocket.domain.ApiStatusCodeEnum
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import au.com.gman.bottlerocket.interfaces.IFileIo
import au.com.gman.bottlerocket.interfaces.IRetrofitApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ApiService @Inject constructor(
    private val fileIo: IFileIo,
    private val retrofitApi: IRetrofitApi
) : IApiService {
    private var listener: IApiResponseListener? = null

    companion object {
        private const val TAG = "ApiService"
        private const val DEFAULT_TIMEOUT_SECONDS = 10L
    }

    override fun setListener(listener: IApiResponseListener) {
        this.listener = listener
    }

    override fun testConnection(baseUrl: String, username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create a temporary Retrofit instance with the test URL
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val okHttpClient =
                    OkHttpClient
                        .Builder()
                        .addInterceptor { chain ->
                            val originalRequest = chain.request()

                            // Add auth header if credentials provided
                            val requestBuilder = if (username.isNotEmpty() && password.isNotEmpty()) {
                                val credentials = Credentials.basic(username, password)
                                originalRequest.newBuilder()
                                    .header("Authorization", credentials)
                            } else {
                                originalRequest.newBuilder()
                            }

                            chain.proceed(requestBuilder.build())
                        }
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api =
                    retrofit
                        .create(IRetrofitApi::class.java)

                // Make the test call
                val httpResponse =
                    api
                        .apiConnectionTest()

                withContext(Dispatchers.Main) {
                    if (httpResponse.isSuccessful && httpResponse.body() != null) {
                        val response = httpResponse.body()!!

                        if (response.isSuccess()) {
                            listener?.onApiConnectionTestSuccess(response)
                        } else {
                            listener?.onApiResponseFailure(response)
                        }
                    } else {
                        // HTTP error - create error response
                        val errorResponse = ConnectionTestResponse(
                            errorCode = httpResponse.code(),
                            errorMessage = "HTTP Error: ${httpResponse.code()} - ${httpResponse.message()}"
                        )
                        Log.e(
                            TAG,
                            "APIService HTTP error: ${httpResponse.code()} - ${httpResponse.message()}"
                        )
                        listener?.onApiResponseFailure(errorResponse)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorResponse = ConnectionTestResponse(
                        errorCode = ApiStatusCodeEnum.UNKNOWN_ERROR.ordinal,
                        errorMessage = "Unable to connect to server."
                    )
                    Log.e(
                        TAG,
                        "APIService error: ${e.message}"
                    )
                    listener?.onApiResponseFailure(errorResponse)
                }
            }
        }
    }

    override fun uploadCapture(
        imageUri: Uri,
        qrCode: String,
        qrBoundingBox: String,
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
                            "image/jpg".toMediaTypeOrNull()
                        )

                val imagePart =
                    MultipartBody
                        .Part
                        .createFormData(
                            "image",
                            file.name,
                            requestFile
                        )

                val qrCodePart = qrCode.toRequestBody("text/plain".toMediaTypeOrNull())
                val qrBoundingBoxPart = qrBoundingBox.toRequestBody("text/plain".toMediaTypeOrNull())

                // Make actual API call
                val httpResponse =
                    retrofitApi
                        .apiCaptureProcess(imagePart, qrCodePart, qrBoundingBoxPart)

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
                        Log.e(
                            TAG,
                            "APIService HTTP error: ${httpResponse.code()} - ${httpResponse.message()}"
                        )
                        listener?.onApiResponseFailure(errorResponse)
                    }
                }

            } catch (e: Exception) {
                // Handle exceptions
                withContext(Dispatchers.Main) {
                    val errorResponse = ProcessCaptureResponse(
                        errorCode = ApiStatusCodeEnum.UNKNOWN_ERROR.ordinal,
                        errorMessage = "Exception: ${e.message}"
                    )
                    Log.e(
                        TAG,
                        "APIService error: ${e.message}"
                    )
                    listener?.onApiResponseFailure(errorResponse)
                }
            }
        }
    }
}