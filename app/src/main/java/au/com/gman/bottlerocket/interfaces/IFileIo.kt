package au.com.gman.bottlerocket.interfaces

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import java.io.File

interface IFileIo {
    fun saveImage(
        bitmap: Bitmap,
        fileNameFormat: String,
        contentResolver: ContentResolver
    )

    fun loadImage(
        uri: Uri,
        cacheDir: File,
        contentResolver: ContentResolver
    ): File

    fun setSaveListener(listener: IFileSaveListener)
}