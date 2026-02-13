package au.com.gman.bottlerocket.activity

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import au.com.gman.bottlerocket.R
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
class RestartRequiredActivity : AppCompatActivity() {

    private lateinit var restartButton: Button
    private lateinit var messageText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restart_required)

        messageText = findViewById(R.id.messageText)
        restartButton = findViewById(R.id.restartButton)

        restartButton.setOnClickListener {
            // Exit the app completely
            finishAffinity()
            exitProcess(0)
        }
    }

    // Prevent back button - they must restart
    override fun onBackPressed() {
        // Do nothing - force them to restart
    }
}