package au.com.gman.bottlerocket.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val DEFAULT_BASE_URL = "http://127.0.0.1:3001/"
    }

    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) {
            prefs.edit { putString(KEY_API_BASE_URL, value) }
        }

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) {
            prefs.edit { putString(KEY_USERNAME, value) }
        }

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) {
            prefs.edit { putString(KEY_PASSWORD, value) }
        }

    fun resetToDefault() {
        apiBaseUrl = DEFAULT_BASE_URL
        username = ""
        password = ""
    }

    fun hasCredentials(): Boolean {
        return username.isNotEmpty() && password.isNotEmpty()
    }
}