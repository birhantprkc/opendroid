package com.opendroid.ai.accessibility

import android.content.Context
import kotlinx.coroutines.delay

object WhatsAppAutomator {

    suspend fun automateSend(message: String): Boolean {
        val service = OpenDroidAccessibilityService.getInstance() ?: return false
        
        // Wait for screen transition
        delay(1500)
        
        // WhatsApp text input id is usually "com.whatsapp:id/entry"
        var typed = service.findAndTypeById("com.whatsapp:id/entry", message)
        if (!typed) {
            // Fallback: search by type/text in active window
            typed = service.findAndType("Type a message", message)
        }
        
        if (!typed) return false
        
        delay(500)
        
        // WhatsApp send button id is usually "com.whatsapp:id/send"
        var clicked = service.findAndClickById("com.whatsapp:id/send")
        if (!clicked) {
            // Try standard labels
            clicked = service.findAndClick("Send") || 
                      service.findAndClick("send") || 
                      service.findAndClick("SEND")
        }
        
        return clicked
    }
}
