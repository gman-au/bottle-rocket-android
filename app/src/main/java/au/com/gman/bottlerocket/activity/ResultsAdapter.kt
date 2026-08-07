import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.contracts.WorkflowSummary

class ResultsAdapter(
    private val onSelectionChanged: (Set<String>) -> Unit
) : ListAdapter<WorkflowSummary, ResultsAdapter.ViewHolder>(DiffCallback()) {

    private val selectedIds = mutableSetOf<String>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.resultTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.title.text = item.workflowName

        val isSelected = selectedIds.contains(item.workflowId)
        holder.itemView.isSelected = isSelected // drives selector-based background, see XML below

        holder.itemView.setOnClickListener {
            if (isSelected) {
                selectedIds.remove(item.workflowId)
            } else {
                selectedIds.add(item.workflowId)
            }
            notifyItemChanged(position) // re-bind this row to reflect new state
            onSelectionChanged(selectedIds.toSet())
        }
    }

    fun getSelectedIds(): Set<String> = selectedIds.toSet()

    class DiffCallback : DiffUtil.ItemCallback<WorkflowSummary>() {
        override fun areItemsTheSame(old: WorkflowSummary, new: WorkflowSummary) = old.workflowId == new.workflowId
        override fun areContentsTheSame(old: WorkflowSummary, new: WorkflowSummary) = old == new
    }
}