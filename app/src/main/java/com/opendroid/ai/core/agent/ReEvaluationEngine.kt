package com.opendroid.ai.core.agent

import com.opendroid.ai.core.llm.LLMProviderFactory
import com.opendroid.ai.core.llm.LLMRequest
import com.opendroid.ai.core.llm.ResponseFormat
import com.opendroid.ai.core.llm.prompts.ReEvalPrompts
import com.opendroid.ai.data.models.Plan
import com.opendroid.ai.data.models.PlanStep
import com.opendroid.ai.data.models.StepStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReEvaluationEngine @Inject constructor(
    private val llmProviderFactory: LLMProviderFactory
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Serializable
    data class ReEvalResult(
        val speech: String,
        val decision: String, // "CONTINUE" | "MODIFY" | "ABANDON"
        val updatedPlan: Plan? = null
    )

    suspend fun evaluateStepResult(
        originalGoal: String,
        completedSteps: List<PlanStep>,
        failedSteps: List<PlanStep>,
        remainingSteps: List<PlanStep>,
        planId: String
    ): ReEvalResult {
        return try {
            val provider = llmProviderFactory.getActiveProvider()
            
            // Format details for the prompt
            val inputDetails = """
                - Original goal: $originalGoal
                - Completed steps: ${json.encodeToString(completedSteps)}
                - Failed steps: ${json.encodeToString(failedSteps)}
                - Remaining steps: ${json.encodeToString(remainingSteps)}
            """.trimIndent()

            val response = provider.complete(
                LLMRequest(
                    systemPrompt = ReEvalPrompts.RE_EVAL_SYSTEM_PROMPT,
                    messages = listOf(
                        com.opendroid.ai.data.models.ChatMessage(
                            id = java.util.UUID.randomUUID().toString(),
                            text = inputDetails,
                            sender = com.opendroid.ai.data.models.ChatMessage.Sender.USER
                        )
                    ),
                    temperature = 0.0f,
                    maxTokens = 1000,
                    responseFormat = ResponseFormat.JSON
                )
            )

            val cleaned = cleanJsonString(response.content)
            json.decodeFromString<ReEvalResult>(cleaned)
        } catch (e: Exception) {
            // Safe fallback if network / parsing fails
            ReEvalResult(
                speech = "Re-evaluation offline. Continuing with existing plan.",
                decision = "CONTINUE",
                updatedPlan = null
            )
        }
    }

    private fun cleanJsonString(raw: String): String {
        var content = raw.trim()
        if (content.startsWith("```json")) {
            content = content.removePrefix("```json")
        }
        if (content.endsWith("```")) {
            content = content.removeSuffix("```")
        }
        return content.trim()
    }
}
