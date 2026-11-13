package au.com.gman.bottlerocket.interfaces

import android.content.ContentResolver
import android.net.Uri
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import java.io.File

interface IApiService {

    fun testConnection(baseUrl: String, username: String, password: String)

    fun uploadCapture(
        imageUri: Uri,
        vendor: String,
        workflows: Set<String>,
        cacheDir: File,
        contentResolver: ContentResolver
    ): Result<ProcessCaptureResponse>

    fun setListener(listener: IApiResponseListener)
}