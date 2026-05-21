package com.opendroid.ai.core.memory

import com.opendroid.ai.data.models.ChatMessage
import com.opendroid.ai.data.models.Plan
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkingMemory @Inject constructor() {
    private val _conversationHistory = mutableListOf<ChatMessage>()
    val conversationHistory: List<ChatMessage> get() = _conversationHistory

    var activePlan: Plan? = null
    var location: String = "Unknown"
    var batteryLevel: Int = 100
    var wifiState: Boolean = true
    var connectivity: String = "WiFi"

    fun addMessage(msg: ChatMessage) {
        _conversationHistory.add(msg)
        if (_conversationHistory.size > 20) {
            _conversationHistory.removeAt(0)
        }
    }

    fun clear() {
        _conversationHistory.clear()
        activePlan = null
    }
}
