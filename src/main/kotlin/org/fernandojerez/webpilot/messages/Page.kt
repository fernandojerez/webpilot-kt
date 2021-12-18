package org.fernandojerez.webpilot.messages

import kotlinx.serialization.Serializable

@Serializable
data class Page(
    val target: Target,
    val frameId: String,
    val errorText: String?,
)
