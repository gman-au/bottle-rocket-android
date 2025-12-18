package au.com.gman.bottlerocket.file

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import au.com.gman.bottlerocket.interfaces.IFileSaveListener
import au.com.gman.bottlerocket.interfaces.IFileSaver
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class FileSaver @Inject constructor() : IFileSaver {

    companion object {
        private const val TAG = "FileSaver"
    }

    private var listener: IFileSaveListener? = null

    override fun setListener(listener: IFileSaveListener) {
        this.listener = listener
    }

    override fun saveImage(
        bitmap: Bitmap,
        fileNameFormat: String,
        contentResolver: ContentResolver
    ) {
        try {
        val name = SimpleDateFormat(fileNameFormat, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BottleRocket")
            }
        }

        val uri =
            contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver
                .openOutputStream(it)?.use { outputStream ->
                    bitmap
                        .compress(
                            Bitmap.CompressFormat.JPEG, 95, outputStream
                        )
                }

            Log.d(TAG, "File saved: $uri")
            listener?.onFileSaveSuccess(uri)
        }
        } catch (exc: Exception) {
            Log.e(TAG, exc.message.toString())
            listener?.onFileSaveFailure()
        }
    }
}