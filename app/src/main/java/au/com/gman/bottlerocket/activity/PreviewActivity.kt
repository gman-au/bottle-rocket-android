package au.com.gman.bottlerocket.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.interfaces.IWorkflowCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class PreviewActivity : AppCompatActivity() {

    @Inject
    lateinit var workflowCache: IWorkflowCache

    private lateinit var imagePreview: ImageView
    private lateinit var sendButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var vendorText: TextView
    private lateinit var loadingIndicator: ProgressBar

    companion object {
        private const val TAG = "PreviewActivity"
    }

    private lateinit var activityLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_preview)

        cancelButton = findViewById(R.id.cancelButton)
        sendButton = findViewById(R.id.sendButton)
        imagePreview = findViewById(R.id.previewView)
        vendorText = findViewById(R.id.textVendor)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        activityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { }

        cancelButton.setOnClickListener { finish() }

        if (intent.action == Intent.ACTION_SEND || intent.getBooleanExtra("isSharedFlow", false)) {
            handleSharedImage()
        } else {
            handleCapturedImage()
        }
    }

    /** Normal flow: CaptureActivity already ran, workflows were loaded back in SplashActivity. */
    private fun handleCapturedImage() {
        val imageUri = intent.getParcelableExtra<Uri>("imagePath")
        val vendor = intent.getStringExtra("vendor") ?: ""

        setupPreview(imageUri, vendor, isSharedFlow = false)
    }

    /** Share flow: no CaptureActivity, no prior workflow fetch — do it now. */
    private fun handleSharedImage() {
        val incomingUri = extractSharedImageUri(intent) ?: intent.getParcelableExtra<Uri>("imagePath")
        val vendor = intent.getStringExtra("vendor") ?: getSendingAppLabel()

        if (incomingUri == null) {
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Persist a stable, app-owned copy so the URI survives across activities/retries,
        // regardless of how long the original share-sheet permission grant lasts.
        val imageUri = if (intent.action == Intent.ACTION_SEND) {
            copyToAppStorage(incomingUri) ?: run {
                Toast.makeText(this, "Couldn't read shared image", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } else {
            incomingUri // already our own copy, from a prior hop
        }

        proceedWithSharedImage(imageUri, vendor)
    }

    private fun extractSharedImageUri(intent: Intent): Uri? {
        if (intent.type?.startsWith("image/") != true) return null
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun setupPreview(imageUri: Uri?, vendor: String, isSharedFlow: Boolean) {
        vendorText.text = vendor

        sendButton.setOnClickListener {
            Log.d(TAG, "User approved send action -> confirming workflow")
            imageUri?.let { uri ->
                val confirmIntent = Intent(this, ConfirmWorkflowActivity::class.java)
                confirmIntent.putExtra("imagePath", uri)
                confirmIntent.putExtra("vendor", vendor)
                confirmIntent.putExtra("isSharedFlow", isSharedFlow)
                activityLauncher.launch(confirmIntent)
            } ?: run {
                Toast.makeText(this, "No image to upload", Toast.LENGTH_SHORT).show()
            }
        }

        if (imageUri != null) {
            imagePreview.setImageURI(imageUri)
        } else {
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        imagePreview.visibility = if (loading) View.GONE else View.VISIBLE
        sendButton.isEnabled = !loading
    }

    private fun getSendingAppLabel(): String {
        val referrerUri = referrer
        val packageName = referrerUri?.host ?: return ""

        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString() // "Google Photos", "Camera", etc.
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    private fun copyToAppStorage(sourceUri: Uri): Uri? {
        return try {
            val destFile = File(cacheDir, "shared_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            FileProvider.getUriForFile(this, "$packageName.fileprovider", destFile)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy shared image", e)
            null
        }
    }

    private fun proceedWithSharedImage(imageUri: Uri, vendor: String) {
        if (workflowCache.isWorkflowsLoaded()) {
            setupPreview(imageUri, vendor = vendor, isSharedFlow = true)
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val result = workflowCache.loadWorkflows()

            result.fold(
                onSuccess = {
                    setLoading(false)
                    setupPreview(imageUri, vendor = vendor, isSharedFlow = true)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load workflows for shared image", error)
                    val warningIntent = Intent(this@PreviewActivity, ServerWarningActivity::class.java)
                    warningIntent.putExtra("imagePath", imageUri)
                    warningIntent.putExtra("vendor", vendor)
                    warningIntent.putExtra("isSharedFlow", true)
                    startActivity(warningIntent)
                    finish()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}