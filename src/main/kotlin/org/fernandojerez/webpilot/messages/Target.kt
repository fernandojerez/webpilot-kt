package org.fernandojerez.webpilot.messages

import kotlinx.serialization.Serializable

@Serializable
data class Target(
    val targetId: String? = null,
    val sessionId: String? = null
)
