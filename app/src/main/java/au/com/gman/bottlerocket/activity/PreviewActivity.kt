package au.com.gman.bottlerocket.activity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.contracts.ApiResponse
import au.com.gman.bottlerocket.contracts.ProcessCaptureResponse
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PreviewActivity : AppCompatActivity() {

    @Inject
    lateinit var apiService: IApiService

    companion object {
        private const val TAG = "PreviewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super
            .onCreate(savedInstanceState)

        setContentView(R.layout.activity_preview)

        val imageUri = intent.getParcelableExtra<Uri>("imagePath")

        val cancelButton = findViewById<ImageButton>(R.id.cancelButton)
        val sendButton = findViewById<ImageButton>(R.id.sendButton)
        val imagePreview = findViewById<ImageView>(R.id.previewView)

        sendButton
            .setOnClickListener {
                Log.d(TAG, "User approved send action!")
                imageUri?.let { uri ->
                    uploadImage(uri)
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
                    Toast.makeText(
                        this@PreviewActivity,
                        "Upload successful: ${response.errorMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(TAG, "Upload success - Code: ${response.errorCode}")
                }

                override fun onApiResponseFailure(response: ProcessCaptureResponse) {
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

    private fun uploadImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Show loading state
                setLoadingState(true)

                val result =
                    apiService
                        .uploadCapture(
                            uri,
                            cacheDir,
                            contentResolver
                        )
            } finally {
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        findViewById<ImageButton>(R.id.sendButton).isEnabled = !isLoading
        findViewById<ImageButton>(R.id.cancelButton).isEnabled = !isLoading

        // If you have a progress bar:
        // findViewById<ProgressBar>(R.id.progressBar).visibility =
        //     if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}