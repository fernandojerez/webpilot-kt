package org.fernandojerez.webpilot.messages

import kotlinx.serialization.Serializable

@Serializable
data class OuterHtml(
    val outerHTML: String
)
