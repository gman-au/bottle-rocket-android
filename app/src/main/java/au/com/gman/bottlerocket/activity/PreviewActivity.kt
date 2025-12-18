package au.com.gman.bottlerocket.activity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import au.com.gman.bottlerocket.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreviewActivity : AppCompatActivity() {

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
        super
            .onDestroy()
    }
}