package org.fernandojerez.webpilot.messages

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val root: Node
)
