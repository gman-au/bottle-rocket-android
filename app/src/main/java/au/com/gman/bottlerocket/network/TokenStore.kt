package au.com.gman.bottlerocket.network

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton

class TokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs =
        EncryptedSharedPreferences
            .create(
                context,
                "auth_tokens",
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences
                    .PrefKeyEncryptionScheme
                    .AES256_SIV,
                EncryptedSharedPreferences
                    .PrefValueEncryptionScheme
                    .AES256_GCM
            )

    val accessToken: String?
        get() =
            prefs
                .getString("access", null)

    val refreshToken: String?
        get() =
            prefs
                .getString("refresh", null)

    val hasSession: Boolean
        get() =
            refreshToken != null

    // commit = true: the old refresh token is already dead server-side, so the new
    // one must be on disk before we carry on. apply() could lose it if the process dies.
    fun save(access: String, refresh: String) =
        prefs
            .edit(commit = true) {
                putString("access", access)
                putString("refresh", refresh)
            }

    fun clear() =
        prefs
            .edit(commit = true) {
                clear()
            }
}