package au.com.gman.bottlerocket.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.gman.bottlerocket.contracts.WorkflowSummary
import au.com.gman.bottlerocket.interfaces.IWorkflowCache
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class WorkflowLoadingActivity : AppCompatActivity() {

    @Inject
    lateinit var workflowCache: IWorkflowCache

    protected fun loadCachesAndProceed() {
        lifecycleScope.launch {
            val result = workflowCache.loadWorkflows()

            result.fold(
                onSuccess = { workflowSummaries -> onWorkflowsLoaded(workflowSummaries) },
                onFailure = { error -> startServerWarningActivity() }
            )
        }
    }

    protected abstract fun onWorkflowsLoaded(workflowSummaries: List<WorkflowSummary>)

    private fun startServerWarningActivity() {
        val intent = Intent(this, ServerWarningActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}