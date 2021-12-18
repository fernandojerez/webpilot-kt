package org.fernandojerez.webpilot.puppeteer.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import org.fernandojerez.webpilot.messages.Document
import org.fernandojerez.webpilot.messages.OuterHtml
import org.fernandojerez.webpilot.messages.Page as PageMessage

class Page(private val page: PageMessage, private val json: Json, private val sendMessage: SendMessage) {

    suspend fun content(): String =
        sendMessage("DOM.getDocument", page.target.sessionId, buildJsonObject { }).let {
            val document = json.decodeFromJsonElement<Document>(it)
            sendMessage("DOM.getOuterHTML", page.target.sessionId, buildJsonObject {
                put("nodeId", document.root.nodeId)
            }).let { outerHtml ->
                json.decodeFromJsonElement<OuterHtml>(outerHtml).outerHTML
            }
        }

}
