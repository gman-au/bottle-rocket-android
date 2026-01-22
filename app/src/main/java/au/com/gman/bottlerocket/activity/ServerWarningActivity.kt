package au.com.gman.bottlerocket.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.qrCode.QrTemplateCache
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ServerWarningActivity : AppCompatActivity() {

    @Inject
    lateinit var templateCache: QrTemplateCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_server_warning)

        val retryButton = findViewById<Button>(R.id.retryButton)
        val continueButton = findViewById<Button>(R.id.continueButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)

        retryButton.setOnClickListener {
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        continueButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        settingsButton.setOnClickListener {
            // First navigate to MainActivity to establish it in the back stack
            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(mainIntent)

            // Then open Settings on top of MainActivity
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)

            finish()
        }
    }
}