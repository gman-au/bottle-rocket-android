import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.domain.BatchPhoto
import coil.load

class PhotoAdapter(
    private val onDelete: (BatchPhoto) -> Unit,
    private val onOpen: (BatchPhoto) -> Unit
) : ListAdapter<BatchPhoto, PhotoAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<BatchPhoto>() {
        override fun areItemsTheSame(a: BatchPhoto, b: BatchPhoto) = a.id == b.id
        override fun areContentsTheSame(a: BatchPhoto, b: BatchPhoto) = a == b
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val thumb: ImageView = v.findViewById(R.id.thumb)
        val delete: View = v.findViewById(R.id.btnDelete)
        val index: TextView = v.findViewById(R.id.indexText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val photo = getItem(position)
        h.thumb.load(photo.uri) { crossfade(true) }      // Coil
        h.index.text = "${position + 1}"
        h.thumb.setOnClickListener { onOpen(photo) }
        h.delete.setOnClickListener { onDelete(photo) }
    }
}