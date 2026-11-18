package au.com.gman.bottlerocket.interfaces

import android.content.ContentResolver
import android.net.Uri
import java.io.File

interface IApiService {

    fun testConnection(baseUrl: String, username: String, password: String)

    suspend fun openBatch(): Result<String>

    suspend fun uploadToBatch(
        batchId: String,
        imageUri: Uri,
        vendor: String,
        cacheDir: File,
        contentResolver: ContentResolver
    ): Result<Unit>

    suspend fun closeBatch(batchId: String, workflows: Set<String>): Result<Unit>

    fun setListener(listener: IApiResponseListener)
}