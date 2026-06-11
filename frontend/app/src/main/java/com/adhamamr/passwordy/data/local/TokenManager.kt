package com.adhamamr.passwordy.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Persists the session JWT and username in Jetpack DataStore so they survive process death.
 * Reads are exposed as [Flow]s; [clearToken] wipes everything on logout.
 *
 * The JWT is encrypted at rest with an Android Keystore key (see [TokenCrypto]) — it is never
 * stored in plaintext. A stored value that fails to decrypt (e.g. left over from an older
 * plaintext build, or after a key reset) is treated as "no token", forcing a fresh login.
 */
class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    // Save token (encrypted via the Android Keystore before it touches disk)
    suspend fun saveToken(token: String) {
        val encrypted = TokenCrypto.encrypt(token)
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = encrypted
        }
    }

    // Save username
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    // Get token as Flow (decrypted on read; null if absent or undecryptable)
    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]?.let { stored ->
            runCatching { TokenCrypto.decrypt(stored) }.getOrNull()
        }
    }

    // Get username as Flow
    val username: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    // Clear all data (logout)
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}