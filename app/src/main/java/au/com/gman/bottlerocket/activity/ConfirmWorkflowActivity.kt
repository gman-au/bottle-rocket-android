package au.com.gman.bottlerocket.activity

import ResultsAdapter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import au.com.gman.bottlerocket.contracts.WorkflowSummary
import au.com.gman.bottlerocket.interfaces.IApiResponse
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import au.com.gman.bottlerocket.interfaces.IWorkflowCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmWorkflowActivity : AppCompatActivity() {

    @Inject
    lateinit var apiService: IApiService
    @Inject
    lateinit var workflowCache: IWorkflowCache

    private lateinit var sendButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: ResultsAdapter

    companion object {
        private const val TAG = "ConfirmWorkflowActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super
            .onCreate(savedInstanceState)

        setContentView(R.layout.activity_confirm_workflow)

        val imageUri = intent.getParcelableExtra<Uri>("imagePath")
        val qrCode = intent.getStringExtra("qrCode") ?: ""
        val qrBoundingBox = intent.getStringExtra("qrBoundingBox") ?: ""
        val vendor = intent.getStringExtra("vendor") ?: ""

        cancelButton = findViewById(R.id.cancelButton)
        sendButton = findViewById(R.id.sendButton)
        progressBar = findViewById(R.id.progressBar)

        sendButton
            .setOnClickListener {
                setLoadingState(true)
                Log.d(TAG, "User approved send action!")
                imageUri?.let { uri ->
                    uploadImage(
                        uri,
                        qrCode,
                        qrBoundingBox,
                        vendor
                    )
                } ?: run {
                    Toast.makeText(this, "No image to upload", Toast.LENGTH_SHORT).show()
                }
            }

        cancelButton
            .setOnClickListener {
                finish()
            }

        recyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.emptyText)

        adapter = ResultsAdapter { selectedIds ->
            // handle selection
            Log.d(TAG, "workflows: ${selectedIds.count()}")
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val workflows = workflowCache.getWorkflows()
        Log.d(TAG, "workflows: ${workflows.count()}")
        showResults(workflows)

        apiService
            .setListener(object : IApiResponseListener {
                override fun onApiProcessCaptureSuccess(response: ProcessCaptureResponse) {
                    setLoadingState(false)
                    Toast.makeText(
                        this@ConfirmWorkflowActivity,
                        "Upload successful: ${response.errorMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(TAG, "Upload success - Code: ${response.errorCode}")
                }

                override fun onApiResponseFailure(response: IApiResponse) {
                    setLoadingState(false)
                    Toast.makeText(
                        this@ConfirmWorkflowActivity,
                        "Upload failed: ${response.errorMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(
                        TAG,
                        "API Error - Code: ${response.errorCode}, Message: ${response.errorMessage}"
                    )
                }
            })
    }

    private fun uploadImage(uri: Uri, qrCode: String, qrBoundingBox: String, vendor: String) {
        lifecycleScope.launch {
            apiService
                .uploadCapture(
                    uri,
                    qrCode,
                    qrBoundingBox,
                    vendor,
                    cacheDir,
                    contentResolver
                )
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        cancelButton.isEnabled = !isLoading
        sendButton.isEnabled = !isLoading
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

    private fun onResultClicked(result: WorkflowSummary) {
        // handle tap — navigate, show detail, etc.
        Log.d(TAG, "result: ${result.workflowId}, ${result.workflowName}")
    }
}