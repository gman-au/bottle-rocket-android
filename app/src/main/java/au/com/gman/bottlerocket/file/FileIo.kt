package au.com.gman.bottlerocket.file

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import au.com.gman.bottlerocket.interfaces.IFileSaveListener
import au.com.gman.bottlerocket.interfaces.IFileIo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class FileIo @Inject constructor() : IFileIo {

    companion object {
        private const val TAG = "FileIo"
    }

    private var listener: IFileSaveListener? = null

    override fun setSaveListener(listener: IFileSaveListener) {
        this.listener = listener
    }

    override fun saveImage(
        bitmap: Bitmap,
        fileNameFormat: String,
        contentResolver: ContentResolver
    ) {
        try {
            val name =
                SimpleDateFormat(fileNameFormat, Locale.US).format(System.currentTimeMillis())

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                // For Android 10+ (API 29+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BottleRocket")
                    put(MediaStore.Images.Media.IS_PENDING, 1) // Mark as pending while writing
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

                // Mark as complete (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(it, contentValues, null, null)
                }

                Log.d(TAG, "File saved: $uri")
                listener?.onFileSaveSuccess(uri)
            }
        } catch (exc: Exception) {
            Log.e(TAG, exc.message.toString())
            listener?.onFileSaveFailure()
        }
    }

    override fun loadImage(
        uri: Uri,
        cacheDir: File,
        contentResolver: ContentResolver
    ): File {
        val tempFile = File(
            cacheDir,
            "capture_${System.currentTimeMillis()}.jpg"
        )

        contentResolver
            .openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

        return tempFile
    }
}