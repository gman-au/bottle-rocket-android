package au.com.gman.bottlerocket.interfaces

import android.content.ContentResolver
import android.graphics.Bitmap

interface IFileSaver {
    fun saveImage(
        bitmap: Bitmap,
        fileNameFormat: String,
        contentResolver: ContentResolver
    )

    fun setListener(listener: IFileSaveListener)
}