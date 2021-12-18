package org.fernandojerez.webpilot.puppeteer.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.fernandojerez.webpilot.messages.Target
import java.io.Closeable
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import org.fernandojerez.webpilot.messages.Page as PageMessage

class Tab(private val target: Target, private val json: Json, private val sendMessage: SendMessage) : Closeable {
    override fun toString(): String = json.encodeToString(target)

    suspend fun <T> navigate(url: String, delaySeconds: Int = 1, perform: suspend Page.() -> T): T =
        sendMessage("Page.navigate", target.sessionId, buildJsonObject {
            put("url", url)
        }).let {
            withContext(Dispatchers.Unconfined) {
                if (delaySeconds > 0) delay(delaySeconds.toDuration(DurationUnit.SECONDS))
                Page(
                    PageMessage(
                        target = target,
                        frameId = (it as JsonObject)["frameId"]?.jsonPrimitive?.content
                            ?: throw RuntimeException("No FrameId"),
                        errorText = (it as JsonObject)["errorText"]?.jsonPrimitive?.content
                    ), json, sendMessage
                ).perform()
            }
        }

    override fun close() {
        runBlocking {
            sendMessage("Target.detachFromTarget", null, json.encodeToJsonElement(target) as JsonObject)
            sendMessage("Target.closeTarget", null, buildJsonObject {
                put("targetId", target.targetId)
            })
        }
    }
}
