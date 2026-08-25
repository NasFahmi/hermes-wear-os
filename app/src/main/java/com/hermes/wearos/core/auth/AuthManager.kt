package com.hermes.wearos.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hermes.wearos.core.network.ApiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("api_token")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val VOICE_LANGUAGE_KEY = stringPreferencesKey("voice_language")
    }

    val token: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[TOKEN_KEY] ?: ApiConfig.DEFAULT_API_TOKEN
    }

    val deviceId: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[DEVICE_ID_KEY]
    }

    val serverUrl: Flow<String> = context.tokenDataStore.data.map { prefs ->
        prefs[SERVER_URL_KEY] ?: ApiConfig.DEFAULT_BASE_URL
    }

    val voiceLanguage: Flow<String> = context.tokenDataStore.data.map { prefs ->
        prefs[VOICE_LANGUAGE_KEY] ?: "id-ID"
    }

    val isAuthenticated: Flow<Boolean> = token.map { !it.isNullOrBlank() }

    suspend fun getToken(): String? = token.first()

    suspend fun getServerUrl(): String = serverUrl.first()

    suspend fun getVoiceLanguage(): String = voiceLanguage.first()

    suspend fun saveToken(newToken: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = newToken.trim()
        }
    }

    suspend fun saveServerUrl(url: String) {
        val sanitizedUrl = if (!url.endsWith("/")) "$url/" else url
        context.tokenDataStore.edit { prefs ->
            prefs[SERVER_URL_KEY] = sanitizedUrl.trim()
        }
    }

    suspend fun saveVoiceLanguage(language: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[VOICE_LANGUAGE_KEY] = language
        }
    }

    suspend fun saveDeviceId(deviceId: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[DEVICE_ID_KEY] = deviceId
        }
    }

    suspend fun clearToken() {
        context.tokenDataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
        }
    }

    suspend fun hasToken(): Boolean = !getToken().isNullOrBlank()
}
