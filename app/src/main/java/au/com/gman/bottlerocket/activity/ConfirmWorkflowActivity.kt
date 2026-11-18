package au.com.gman.bottlerocket.activity

import ResultsAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.contracts.WorkflowSummary
import au.com.gman.bottlerocket.domain.BatchPhoto
import au.com.gman.bottlerocket.interfaces.IApiService
import au.com.gman.bottlerocket.interfaces.IBatchRepository
import au.com.gman.bottlerocket.interfaces.IWorkflowCache
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfirmWorkflowActivity : AppCompatActivity() {

    @Inject
    lateinit var apiService: IApiService

    @Inject
    lateinit var workflowCache: IWorkflowCache

    @Inject
    lateinit var batchRepository: IBatchRepository

    private lateinit var sendButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var countText: TextView
    private lateinit var progressText: TextView
    private lateinit var adapter: ResultsAdapter

    private lateinit var loadingOverlay: FrameLayout

    private var isSharedFlow = false

    // upload state
    private val pending = ArrayDeque<BatchPhoto>()
    private var inFlight: BatchPhoto? = null
    private var workflows: Set<String> = emptySet()
    private var totalToSend = 0

    companion object {
        private const val TAG = "ConfirmWorkflowActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super
            .onCreate(savedInstanceState)

        setContentView(R.layout.activity_confirm_workflow)

        isSharedFlow = intent.getBooleanExtra("isSharedFlow", false)

        cancelButton = findViewById(R.id.cancelButton)
        sendButton = findViewById(R.id.sendButton)
        progressBar = findViewById(R.id.progressBar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        recyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.emptyText)
        countText = findViewById(R.id.countText)
        progressText = findViewById(R.id.progressText)

        sendButton.setOnClickListener {
            if (batchRepository.photos.value.isEmpty() && batchRepository.remoteBatchId == null) return@setOnClickListener
            val workflows = adapter.getSelectedIds()
            setLoadingState(true)

            lifecycleScope.launch {
                sendBatch(workflows).fold(
                    onSuccess = { total ->
                        setLoadingState(false)
                        Toast.makeText(this@ConfirmWorkflowActivity,
                            "Sent $total photos", Toast.LENGTH_SHORT).show()
                        if (isSharedFlow) finishAffinity() else returnToCapture()
                    },
                    onFailure = { e ->
                        setLoadingState(false)
                        Toast.makeText(this@ConfirmWorkflowActivity,
                            "${e.message}. Tap Send to retry.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        cancelButton.setOnClickListener { finish() }     // back to review

        adapter = ResultsAdapter { ids -> Log.d(TAG, "workflows: ${ids.count()}") }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        showResults(workflowCache.getWorkflows())
    }

    private fun setLoadingState(isLoading: Boolean) {
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        cancelButton.isEnabled = !isLoading
        sendButton.isEnabled = !isLoading && (batchRepository.photos.value.count()) > 0
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun showResults(results: List<WorkflowSummary>) {
        if (results.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            adapter.submitList(results)
        }
    }

    private fun returnToCapture() {
        val intent = Intent(this, CaptureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private suspend fun sendBatch(workflows: Set<String>): Result<Int> {
        val photos = batchRepository.photos.value
        val total = photos.size

        var batchId = batchRepository.remoteBatchId
        if (batchId == null) {
            batchId = apiService.openBatch().getOrElse { return Result.failure(it) }
            batchRepository.remoteBatchId = batchId
        }

        photos.forEachIndexed { i, photo ->
            progressText.text = "Sending ${i + 1} of $total"
            apiService.uploadToBatch(batchId, photo.uri, photo.vendor, cacheDir, contentResolver)
                .onFailure { return Result.failure(it) }
            batchRepository.remove(photo.id)
        }

        progressText.text = "Running workflows…"
        apiService.closeBatch(batchId, workflows).onFailure { return Result.failure(it) }

        batchRepository.clear()
        return Result.success(total)
    }

}