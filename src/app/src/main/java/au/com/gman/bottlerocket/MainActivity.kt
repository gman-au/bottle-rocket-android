package au.com.gman.bottlerocket

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import au.com.gman.bottlerocket.imaging.QrCodeDetector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var qrCodeDetector: QrCodeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val beginCaptureButton = findViewById<Button>(R.id.captureButton)

        beginCaptureButton.setOnClickListener {
            startActivity(Intent(this, CaptureActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}