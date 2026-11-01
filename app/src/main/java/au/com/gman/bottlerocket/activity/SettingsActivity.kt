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
import androidx.lifecycle.lifecycleScope
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.settings.AppSettings
import au.com.gman.bottlerocket.interfaces.IApiResponse
import au.com.gman.bottlerocket.interfaces.IApiResponseListener
import au.com.gman.bottlerocket.interfaces.IApiService
import au.com.gman.bottlerocket.network.AuthService
import au.com.gman.bottlerocket.network.TokenStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var apiService: IApiService

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var tokenStore: TokenStore

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

        // password is deliberately not restored: it is never stored
        passwordInput.hint = "Password (used to sign in, not stored)"

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

                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                val nothingChanged =
                    tokenStore.hasSession &&
                            url == appSettings.apiBaseUrl &&
                            username == appSettings.username

                if (password.isEmpty()) {
                    if (nothingChanged) {
                        finish()
                    } else {
                        Toast.makeText(this, "Enter your password to sign in", Toast.LENGTH_SHORT)
                            .show()
                    }
                    return@setOnClickListener
                }

                setLoadingState(true)

                lifecycleScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            authService
                                .signIn(
                                    url,
                                    username,
                                    password
                                )
                        }

                        if (result == null) {
                            setLoadingState(false)
                            Toast.makeText(
                                this@SettingsActivity,
                                "Invalid username or password",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        passwordInput
                            .text
                            .clear()

                        val intent =
                            Intent(this@SettingsActivity, RestartRequiredActivity::class.java)

                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        startActivity(intent)
                        finish()
                    } catch (e: IOException) {
                        setLoadingState(false)
                        Toast
                            .makeText(
                                this@SettingsActivity,
                                "Can't reach the server",
                                Toast.LENGTH_LONG
                            ).show()
                    }
                }
            }

        resetButton
            .setOnClickListener {
                setLoadingState(true)
                lifecycleScope.launch {
                    // Revoke on the current server first, because resetting changes the URL
                    withContext(Dispatchers.IO) { authService.logout() }

                    appSettings.resetToDefault()
                    urlInput.setText(appSettings.apiBaseUrl)
                    usernameInput.setText("")
                    passwordInput.text.clear()

                    setLoadingState(false)
                    Toast.makeText(this@SettingsActivity, "Reset to default", Toast.LENGTH_SHORT)
                        .show()
                }
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
        usernameInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        testConnectionButton.isEnabled = !isLoading
        cancelButton.isEnabled = !isLoading
        saveButton.isEnabled = !isLoading
        resetButton.isEnabled = !isLoading
        urlInput.isEnabled = !isLoading
    }
}