package au.com.gman.bottlerocket.network

import au.com.gman.bottlerocket.contracts.ConnectionTestResponse
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import au.com.gman.bottlerocket.contracts.CloseBatchRequest
import au.com.gman.bottlerocket.domain.ApiStatusCodeEnum
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import au.com.gman.bottlerocket.interfaces.IFileIo
import au.com.gman.bottlerocket.interfaces.IRetrofitApi
import au.com.gman.bottlerocket.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class ApiService @Inject constructor(
    private val fileIo: IFileIo,
    private val retrofitApi: IRetrofitApi,
    private val authService: AuthService,
    private val appSettings: AppSettings,
    private val tokenStore: TokenStore
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
            var accessToken: String? = null
            try {

                val sameAsSaved =
                    baseUrl.trimEnd('/') == appSettings.apiBaseUrl.trimEnd('/') &&
                            username == appSettings.username

                val useSavedSession = password.isEmpty() && sameAsSaved && tokenStore.hasSession

                val api: IRetrofitApi =
                    if (useSavedSession) {
                        retrofitApi
                    } else {
                        if (username.isEmpty() || password.isEmpty()) {
                            reportTestFailure(400, "Enter your username and password to test.")
                            return@launch
                        }

                        accessToken = authService.loginAgainst(baseUrl, username, password)

                        if (accessToken == null) {
                            reportTestFailure(401, "Invalid username or password.")
                            return@launch
                        }

                        buildTestApi(baseUrl, accessToken)
                    }

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
            } finally {
                accessToken?.let { authService.logoutAgainst(baseUrl, it) }
            }
        }
    }

    private suspend fun reportTestFailure(code: Int, message: String) =
        withContext(Dispatchers.Main) {
            listener?.onApiResponseFailure(
                ConnectionTestResponse(errorCode = code, errorMessage = message)
            )
        }

    private fun buildTestApi(baseUrl: String, token: String): IRetrofitApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                )
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IRetrofitApi::class.java)
    }

    override suspend fun openBatch(): Result<String> = withContext(Dispatchers.IO) {
        apiCall("Open batch") {
            val r = retrofitApi.apiBatchOpen()
            val id = r.body()?.batchId
            if (!r.isSuccessful || id.isNullOrBlank())
                throw IOException("HTTP ${r.code()} ${r.message()}")
            id
        }
    }

    override suspend fun uploadToBatch(
        batchId: String, imageUri: Uri, vendor: String,
        cacheDir: File, contentResolver: ContentResolver
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var file: File? = null
        try {
            apiCall("Upload") {
                val f = fileIo.loadImage(imageUri, cacheDir, contentResolver).also { file = it }
                val imagePart = MultipartBody.Part.createFormData(
                    "image", f.name, f.asRequestBody("image/jpg".toMediaTypeOrNull())
                )
                val text = "text/plain".toMediaTypeOrNull()
                val r = retrofitApi.apiCaptureProcess(
                    imagePart, vendor.toRequestBody(text), batchId.toRequestBody(text)
                )
                val body = r.body()
                if (!r.isSuccessful || body == null)
                    throw IOException("HTTP ${r.code()} ${r.message()}")
                if (!body.isSuccess()) throw IOException(body.errorMessage)
            }
        } finally {
            file?.delete()
        }
    }

    override suspend fun closeBatch(batchId: String, workflows: Set<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            apiCall("Close batch") {
                val r = retrofitApi.apiBatchClose(CloseBatchRequest(batchId, workflows.toList()))
                if (!r.isSuccessful) throw IOException("HTTP ${r.code()} ${r.message()}")
            }
        }

    private suspend fun <T> apiCall(what: String, block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$what failed: ${e.message}")
            Result.failure(Exception("$what failed: ${e.message}", e))
        }
}