package au.com.gman.bottlerocket.imaging

import android.net.Uri
import au.com.gman.bottlerocket.domain.BatchPhoto
import au.com.gman.bottlerocket.interfaces.IBatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchRepository @Inject constructor() : IBatchRepository {
    private val _photos = MutableStateFlow<List<BatchPhoto>>(emptyList())
    override val photos: StateFlow<List<BatchPhoto>> = _photos.asStateFlow()

    override fun add(uri: Uri, vendor: String) = _photos.update { it + BatchPhoto(uri = uri, vendor = vendor) }
    override fun remove(id: String) = _photos.update { list -> list.filterNot { it.id == id } }
    override fun clear() { _photos.value = emptyList() }
}