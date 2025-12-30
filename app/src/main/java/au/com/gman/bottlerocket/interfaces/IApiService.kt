package au.com.gman.bottlerocket.interfaces

import android.content.ContentResolver
import android.net.Uri
import java.io.File

interface IApiService {
    fun uploadCapture(
        imageUri: Uri,
        cacheDir: File,
        contentResolver: ContentResolver
    )

    fun setListener(listener: IApiResponseListener)
}