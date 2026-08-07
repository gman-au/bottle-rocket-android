package au.com.gman.bottlerocket.interfaces

import android.content.ContentResolver
import android.net.Uri
import java.io.File

interface IApiService {

    fun testConnection(baseUrl: String, username: String, password: String)

    fun uploadCapture(
        imageUri: Uri,
        vendor: String,
        workflows: Set<String>,
        cacheDir: File,
        contentResolver: ContentResolver
    )

    fun setListener(listener: IApiResponseListener)
}