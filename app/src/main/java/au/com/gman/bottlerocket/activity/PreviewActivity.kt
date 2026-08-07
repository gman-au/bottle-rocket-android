package au.com.gman.bottlerocket.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import au.com.gman.bottlerocket.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreviewActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var sendButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var vendorText: TextView

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
        val vendor = intent.getStringExtra("vendor") ?: ""

        cancelButton = findViewById(R.id.cancelButton)
        sendButton = findViewById(R.id.sendButton)
        imagePreview = findViewById(R.id.previewView)
        vendorText = findViewById(R.id.textVendor)

        vendorText.text = vendor

        val activityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
                result -> {}
        }

        sendButton
            .setOnClickListener {
                Log.d(TAG, "User approved send action -> confirming workflow")
                imageUri?.let { uri ->
                    val intent = Intent(this@PreviewActivity, ConfirmWorkflowActivity::class.java)
                    intent.putExtra("imagePath", imageUri);
                    intent.putExtra("qrCode", qrCode)
                    intent.putExtra("qrBoundingBox", qrBoundingBox)
                    intent.putExtra("vendor", vendor)
                    activityLauncher.launch(intent);
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
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}