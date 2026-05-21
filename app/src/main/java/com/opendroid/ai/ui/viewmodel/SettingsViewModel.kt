package com.opendroid.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opendroid.ai.data.models.LLMConfig
import com.opendroid.ai.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.opendroid.ai.core.llm.LLMRequest
import com.opendroid.ai.core.llm.ResponseFormat
import com.opendroid.ai.data.models.ChatMessage
import dagger.Lazy

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val llmProviderFactory: Lazy<com.opendroid.ai.core.llm.LLMProviderFactory>
) : ViewModel() {

    val llmConfig: StateFlow<LLMConfig> = settingsRepository.llmConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LLMConfig()
        )

    fun updateActiveProvider(provider: String) {
        viewModelScope.launch {
            settingsRepository.updateConfig { current ->
                current.copy(activeProvider = provider)
            }
        }
    }

    fun updateActiveModel(model: String) {
        viewModelScope.launch {
            settingsRepository.updateConfig { current ->
                current.copy(activeModel = model)
            }
        }
    }

    fun updateApiKey(providerName: String, key: String) {
        viewModelScope.launch {
            settingsRepository.updateConfig { current ->
                val keys = current.apiKeys.toMutableMap()
                keys[providerName] = key
                current.copy(apiKeys = keys)
            }
        }
    }

    fun updateElevenLabsApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.updateConfig { current ->
                current.copy(elevenLabsApiKey = key)
            }
        }
    }

    fun updateElevenLabsVoiceId(voiceId: String) {
        viewModelScope.launch {
            settingsRepository.updateConfig { current ->
                current.copy(elevenLabsVoiceId = voiceId)
            }
        }
    }

    fun updateOllamaUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.updateConfig { current ->
                current.copy(ollamaUrl = url)
            }
        }
    }

    fun testProviderLatency(providerName: String) {
        viewModelScope.launch {
            try {
                val factory = llmProviderFactory.get()
                val provider = factory.getProviderByName(providerName)
                if (provider.isAvailable()) {
                    val request = LLMRequest(
                        systemPrompt = "You are a speed test server. Respond with 'pong'.",
                        messages = listOf(ChatMessage(id = "1", text = "ping", sender = ChatMessage.Sender.USER)),
                        responseFormat = ResponseFormat.TEXT
                    )
                    val response = provider.complete(request)
                    settingsRepository.updateConfig { current ->
                        val updatedBenchmarks = current.latencyBenchmarks.toMutableMap()
                        updatedBenchmarks[providerName] = response.latencyMs
                        current.copy(latencyBenchmarks = updatedBenchmarks)
                    }
                }
            } catch (e: Exception) {
                // Keep the record but fail with high number
                settingsRepository.updateConfig { current ->
                    val updatedBenchmarks = current.latencyBenchmarks.toMutableMap()
                    updatedBenchmarks[providerName] = 9999L
                    current.copy(latencyBenchmarks = updatedBenchmarks)
                }
            }
        }
    }
}
