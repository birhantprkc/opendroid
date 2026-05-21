package com.opendroid.ai.core.agent

import com.opendroid.ai.core.llm.LLMProviderFactory
import com.opendroid.ai.core.llm.LLMRequest
import com.opendroid.ai.core.llm.ResponseFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentClassifier @Inject constructor(
    private val llmProviderFactory: LLMProviderFactory
) {
    suspend fun isComplex(query: String): Boolean {
        // Quick local heuristic check to avoid network usage for trivially simple prompts
        val actionTriggers = listOf("then", "after", "and", "first", "next", "then send", "book", "schedule", "routine")
        val hasComplexKeywords = actionTriggers.any { query.contains(it, ignoreCase = true) }
        
        // If it doesn't look complex heuristically, it's simple
        if (!hasComplexKeywords) return false

        return try {
            val provider = llmProviderFactory.getActiveProvider()
            val prompt = """
                Classify the user's intent: "$query".
                Is this request complex (requires multiple actions/tasks sequentially or in parallel) or simple (only one direct response or single action)?
                Return strictly a single word: "COMPLEX" or "SIMPLE".
            """.trimIndent()

            val response = provider.complete(
                LLMRequest(
                    systemPrompt = "You are an intent classification routing helper.",
                    messages = emptyList(),
                    temperature = 0.0f,
                    maxTokens = 5,
                    responseFormat = ResponseFormat.TEXT
                )
            )
            response.content.contains("COMPLEX", ignoreCase = true)
        } catch (e: Exception) {
            // Fallback to keyword heuristics if LLM is unreachable or offline
            hasComplexKeywords
        }
    }
}
