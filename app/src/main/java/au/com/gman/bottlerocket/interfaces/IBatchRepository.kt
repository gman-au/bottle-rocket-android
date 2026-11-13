package au.com.gman.bottlerocket.interfaces

import android.net.Uri
import au.com.gman.bottlerocket.domain.BatchPhoto
import kotlinx.coroutines.flow.StateFlow

interface IBatchRepository {
    val photos: StateFlow<List<BatchPhoto>>
    fun add(uri: Uri, vendor: String)
    fun remove(id: String)
    fun clear()
}