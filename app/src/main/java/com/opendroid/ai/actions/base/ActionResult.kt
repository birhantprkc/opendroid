package com.opendroid.ai.actions.base

import kotlinx.serialization.Serializable

@Serializable
data class ActionResult(
    val success: Boolean,
    val data: String? = null,
    val error: String? = null,
    val fallbackExecuted: Boolean = false
)
