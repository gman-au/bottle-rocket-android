package au.com.gman.bottlerocket.activity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import au.com.gman.bottlerocket.interfaces.IApiResponse
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PreviewActivity : AppCompatActivity() {

    @Inject
    lateinit var apiService: IApiService

    private lateinit var imagePreview: ImageView
    private lateinit var sendButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "PreviewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super
            .onCreate(savedInstanceState)

        setContentView(R.layout.activity_preview)

        val imageUri = intent.getParcelableExtra<Uri>("imagePath")
        val qrCode = intent.getStringExtra("qrCode") ?: ""
        val qrBoundingBox = intent.getStringExtra("qrBoundingBox") ?: ""

        cancelButton = findViewById(R.id.cancelButton)
        sendButton = findViewById(R.id.sendButton)
        imagePreview = findViewById(R.id.previewView)
        progressBar = findViewById(R.id.progressBar)

        sendButton
            .setOnClickListener {
                setLoadingState(true)
                Log.d(TAG, "User approved send action!")
                imageUri?.let { uri ->
                    uploadImage(
                        uri,
                        qrCode,
                        qrBoundingBox
                    )
                } ?: run {
                    Toast.makeText(this, "No image to upload", Toast.LENGTH_SHORT).show()
                }
            }

        cancelButton
            .setOnClickListener {
                finish()
            }

        // Set the image
        if (imageUri != null) {
            imagePreview.setImageURI(imageUri)
        } else {
            // Handle the case where URI is null
            runOnUiThread {
                Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show()
            }
        }

        apiService
            .setListener(object : IApiResponseListener {
                override fun onApiProcessCaptureSuccess(response: ProcessCaptureResponse) {
                    setLoadingState(false)
                    Toast.makeText(
                        this@PreviewActivity,
                        "Upload successful: ${response.errorMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(TAG, "Upload success - Code: ${response.errorCode}")
                }

                override fun onApiResponseFailure(response: IApiResponse) {
                    setLoadingState(false)
                    Toast.makeText(
                        this@PreviewActivity,
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

    private fun uploadImage(uri: Uri, qrCode: String, qrBoundingBox: String) {
        lifecycleScope.launch {
            apiService
                .uploadCapture(
                    uri,
                    qrCode,
                    qrBoundingBox,
                    cacheDir,
                    contentResolver
                )
        }
    }


    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        imagePreview.isEnabled = !isLoading
        imagePreview.setAlpha(if (isLoading) 0.5f else 1.0f)
        cancelButton.isEnabled = !isLoading
        sendButton.isEnabled = !isLoading
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}