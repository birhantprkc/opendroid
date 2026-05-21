package com.opendroid.ai.core.memory

import com.opendroid.ai.data.models.ChatMessage
import com.opendroid.ai.data.models.Memory
import com.opendroid.ai.data.models.MemoryType
import com.opendroid.ai.data.repository.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryExtractor @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    private val patterns = listOf(
        Regex("my name is ([a-zA-Z\\s]+)", RegexOption.IGNORE_CASE) to "user_name",
        Regex("my wife's name is ([a-zA-Z\\s]+)", RegexOption.IGNORE_CASE) to "wife_name",
        Regex("my husband's name is ([a-zA-Z\\s]+)", RegexOption.IGNORE_CASE) to "husband_name",
        Regex("I live in ([a-zA-Z\\s,]+)", RegexOption.IGNORE_CASE) to "user_location",
        Regex("my office address is ([a-zA-Z0-9\\s,]+)", RegexOption.IGNORE_CASE) to "office_address",
        Regex("my home address is ([a-zA-Z0-9\\s,]+)", RegexOption.IGNORE_CASE) to "home_address",
        Regex("my phone number is ([0-9+\\-]+)", RegexOption.IGNORE_CASE) to "user_phone"
    )

    suspend fun extractFacts(messages: List<ChatMessage>) {
        messages.forEach { msg ->
            if (msg.sender == ChatMessage.Sender.USER) {
                // Heuristic pattern checking
                patterns.forEach { (regex, key) ->
                    val match = regex.find(msg.text)
                    if (match != null) {
                        val value = match.groupValues[1].trim()
                        memoryRepository.saveMemory(
                            Memory(key = key, value = value, type = MemoryType.SEMANTIC)
                        )
                    }
                }

                // Dynamic extraction helper
                val wifeMatch = Regex("([a-zA-Z]+) is my wife", RegexOption.IGNORE_CASE).find(msg.text)
                if (wifeMatch != null) {
                    memoryRepository.saveMemory(
                        Memory(key = "wife_name", value = wifeMatch.groupValues[1].trim(), type = MemoryType.SEMANTIC)
                    )
                }

                val husbandMatch = Regex("([a-zA-Z]+) is my husband", RegexOption.IGNORE_CASE).find(msg.text)
                if (husbandMatch != null) {
                    memoryRepository.saveMemory(
                        Memory(key = "husband_name", value = husbandMatch.groupValues[1].trim(), type = MemoryType.SEMANTIC)
                    )
                }
            }
        }
    }
}
