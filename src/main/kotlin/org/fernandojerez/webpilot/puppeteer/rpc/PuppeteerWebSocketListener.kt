package org.fernandojerez.webpilot.puppeteer.rpc

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class PuppeteerWebSocketListener(private val channel: SendChannel<Any>) : WebSocketListener() {

    enum class PuppeteerMessage {
        OPEN,
        CLOSING
    }

    data class PuppeteerFailure(
        val message: String
    )

    data class PuppeteerException(
        val id: Int,
        val message: JsonObject
    )

    data class PuppeteerResult(
        val id: Int,
        val result: JsonObject
    )

    private fun sendBlocking(message: Any) {
        channel.trySendBlocking(message)
            .onFailure { throw it!! }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) =
        sendBlocking(PuppeteerMessage.CLOSING)

    override fun onOpen(webSocket: WebSocket, response: Response) = sendBlocking(PuppeteerMessage.OPEN)

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        sendBlocking(
            PuppeteerFailure(
                message = response?.body?.string()
                    ?: t.message
                    ?: t::class.simpleName
                    ?: "Unknown Error"
            )
        )
        sendBlocking(PuppeteerMessage.CLOSING)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val message = Json.parseToJsonElement(text) as JsonObject
        val id = message["id"]?.jsonPrimitive?.content?.toInt() ?: return
        if (message["error"] != null) {
            sendBlocking(
                PuppeteerException(
                    id = id,
                    message = message["error"]!!.jsonObject
                )
            )
            return
        }
        val result = message["result"]?.jsonObject ?: return
        sendBlocking(
            PuppeteerResult(
                id = id,
                result = result
            )
        )
    }

}
