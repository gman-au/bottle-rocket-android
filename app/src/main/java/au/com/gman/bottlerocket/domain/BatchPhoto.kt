package au.com.gman.bottlerocket.domain

import android.net.Uri
import java.util.UUID

data class BatchPhoto(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val vendor: String,
    val capturedAt: Long = System.currentTimeMillis()
)