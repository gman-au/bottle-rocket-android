package au.com.gman.bottlerocket.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import au.com.gman.bottlerocket.BuildConfig
import au.com.gman.bottlerocket.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super
            .onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val beginCaptureButton = findViewById<Button>(R.id.captureButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)
        val versionNumberText = findViewById<TextView>(R.id.versionNumber)

        val exitButton = findViewById<Button>(R.id.exitButton)

        beginCaptureButton
            .setOnClickListener {
                startActivity(Intent(this, CaptureActivity::class.java))
            }

        settingsButton
            .setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

        exitButton
            .setOnClickListener {
                finish()
            }

        versionNumberText.text = BuildConfig.VERSION_NAME
    }

    override fun onDestroy() {
        super
            .onDestroy()
    }
}