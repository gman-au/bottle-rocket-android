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
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import au.com.gman.bottlerocket.contracts.WorkflowSummary
import au.com.gman.bottlerocket.domain.BatchPhoto
import au.com.gman.bottlerocket.interfaces.IApiResponse
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import au.com.gman.bottlerocket.interfaces.IBatchRepository
import au.com.gman.bottlerocket.interfaces.IWorkflowCache
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.view.isVisible

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
    private var selectedWorkflows: Set<String> = emptySet()

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
            if (batchRepository.photos.value.isEmpty()) return@setOnClickListener
            workflows = adapter.getSelectedIds()
            pending.clear(); pending.addAll(batchRepository.photos.value)
            setLoadingState(true)
            sendNext()
        }

        cancelButton.setOnClickListener { finish() }     // back to review

        adapter = ResultsAdapter { ids -> Log.d(TAG, "workflows: ${ids.count()}") }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        showResults(workflowCache.getWorkflows())

        apiService.setListener(object : IApiResponseListener {
            override fun onApiProcessCaptureSuccess(response: ProcessCaptureResponse) {
                // this photo is done: take it out of the batch so a retry skips it
                inFlight?.let { batchRepository.remove(it.id) }
                inFlight = null
                sendNext()
            }

            override fun onApiResponseFailure(response: IApiResponse) {
                val remaining = pending.size + (if (inFlight != null) 1 else 0)
                val sent = totalToSend - remaining
                inFlight = null
                pending.clear()
                setLoadingState(false)
                Toast.makeText(
                    this@ConfirmWorkflowActivity,
                    "Sent $sent of $totalToSend. $remaining left, tap Send to retry. (${response.errorMessage})",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(
                    TAG,
                    "API Error - Code: ${response.errorCode}, Message: ${response.errorMessage}"
                )
            }
        })
    }

    private val uploading get() = loadingOverlay.isVisible

    private fun startSending() {
        val photos = batchRepository.photos.value.orEmpty()
        if (photos.isEmpty()) {
            Toast.makeText(this, "No images to upload", Toast.LENGTH_SHORT).show()
            return
        }
        selectedWorkflows = adapter.getSelectedIds()     // snapshot once for the whole batch
        pending.clear()
        pending.addAll(photos)
        totalToSend = photos.size
        setLoadingState(true)
        sendNext()
    }

    private fun sendNext() {
        val next = pending.removeFirstOrNull()
        if (next == null) {
            setLoadingState(false)
            Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show()
            if (isSharedFlow) finishAffinity() else returnToCapture()
            return
        }
        inFlight = next
        apiService.uploadCapture(next.uri, next.vendor, workflows, cacheDir, contentResolver)
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

}