package au.com.gman.bottlerocket.activity

import android.content.Intent
import au.com.gman.bottlerocket.contracts.ConnectionTestResponse
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.settings.AppSettings
import au.com.gman.bottlerocket.interfaces.IApiResponse
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var apiService: IApiService

    private lateinit var urlInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText

    private lateinit var scanQrButton: Button
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    private lateinit var testConnectionButton: Button

    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "SettingsActivity"
        private const val QR_SCAN_REQUEST = 101
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == QR_SCAN_REQUEST && resultCode == RESULT_OK && data != null) {
            data.getStringExtra("server")?.let { urlInput.setText(it) }
            data.getStringExtra("username")?.let { usernameInput.setText(it) }
            data.getStringExtra("password")?.let { passwordInput.setText(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        urlInput = findViewById(R.id.urlInput)
        saveButton = findViewById(R.id.saveButton)
        resetButton = findViewById(R.id.resetButton)
        testConnectionButton = findViewById(R.id.testConnectionButton)
        cancelButton = findViewById(R.id.cancelButton)
        progressBar = findViewById(R.id.progressBar)
        usernameInput = findViewById(R.id.credentialsUserInput)
        passwordInput = findViewById(R.id.credentialsPasswordInput)
        scanQrButton = findViewById(R.id.scanQrButton)

        // Load current settings
        urlInput.setText(appSettings.apiBaseUrl)
        usernameInput.setText(appSettings.username)
        passwordInput.setText(appSettings.password)

        cancelButton
            .setOnClickListener {
                finish()
            }

        scanQrButton.setOnClickListener {
            val intent = Intent(this, QrAuthActivity::class.java)
            startActivityForResult(intent, QR_SCAN_REQUEST)
        }

        testConnectionButton
            .setOnClickListener {
                setLoadingState(true)

                val username =
                    usernameInput
                        .text
                        .toString()
                        .trim()

                val password =
                    passwordInput
                        .text
                        .toString()
                        .trim()

                val url =
                    urlInput
                        .text
                        .toString()
                        .trim()

                apiService
                    .testConnection(url, username, password)
            }

        saveButton
            .setOnClickListener {
                var url =
                    urlInput
                        .text
                        .toString()
                        .trim()

                // Validation
                if (url.isEmpty()) {
                    Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Ensure it ends with /
                if (!url.endsWith("/")) {
                    url += "/"
                }

                appSettings.apiBaseUrl = url
                appSettings.username = usernameInput.text.toString().trim()
                appSettings.password = passwordInput.text.toString().trim()

                val intent = Intent(this, RestartRequiredActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

        resetButton
            .setOnClickListener {
                appSettings
                    .resetToDefault()

                urlInput.setText(appSettings.apiBaseUrl)
                usernameInput.setText(appSettings.username)
                passwordInput.setText(appSettings.password)

                Toast
                    .makeText(this, "Reset to default", Toast.LENGTH_SHORT)
                    .show()
            }

        apiService
            .setListener(object : IApiResponseListener {
                override fun onApiConnectionTestSuccess(response: ConnectionTestResponse) {
                    setLoadingState(false)
                    Toast
                        .makeText(
                            this@SettingsActivity,
                            "Connection test successful!",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                    Log.d(TAG, "Connection success - Code: ${response.errorCode}")
                }

                override fun onApiResponseFailure(response: IApiResponse) {
                    setLoadingState(false)
                    Toast
                        .makeText(
                            this@SettingsActivity,
                            "Connection test failed: ${response.errorMessage}",
                            Toast.LENGTH_LONG
                        )
                        .show()
                    Log.e(
                        TAG,
                        "API Error - Code: ${response.errorCode}, Message: ${response.errorMessage}"
                    )
                }
            })
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        testConnectionButton.isEnabled = !isLoading
        cancelButton.isEnabled = !isLoading
        saveButton.isEnabled = !isLoading
        resetButton.isEnabled = !isLoading
        urlInput.isEnabled = !isLoading
    }
}