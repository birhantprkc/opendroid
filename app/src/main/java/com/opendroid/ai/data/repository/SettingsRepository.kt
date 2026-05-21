package com.opendroid.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opendroid.ai.data.models.LLMConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val llmConfigKey = stringPreferencesKey("llm_config")

    val llmConfig: Flow<LLMConfig> = context.dataStore.data.map { preferences ->
        val configStr = preferences[llmConfigKey]
        if (configStr != null) {
            try {
                json.decodeFromString<LLMConfig>(configStr)
            } catch (e: Exception) {
                LLMConfig()
            }
        } else {
            LLMConfig()
        }
    }

    suspend fun updateConfig(update: (LLMConfig) -> LLMConfig) {
        context.dataStore.edit { preferences ->
            val currentStr = preferences[llmConfigKey]
            val currentConfig = if (currentStr != null) {
                try {
                    json.decodeFromString<LLMConfig>(currentStr)
                } catch (e: Exception) {
                    LLMConfig()
                }
            } else {
                LLMConfig()
            }
            val newConfig = update(currentConfig)
            preferences[llmConfigKey] = json.encodeToString(newConfig)
        }
    }
}
