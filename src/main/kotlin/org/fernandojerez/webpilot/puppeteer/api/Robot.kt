package org.fernandojerez.webpilot.puppeteer.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.fernandojerez.webpilot.puppeteer.rpc.PuppeteerWebSocketListener
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

typealias SendMessage = suspend (method: String, sessionId: String?, payload: JsonObject) -> JsonElement

class Robot(private val process: Process, port: Int) : Closeable {

    private var eventLoop: CompletableFuture<Void>
    private var channel = Channel<Any>()
    private var websocket: WebSocket
    private var counter = AtomicInteger()
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val producers = mutableMapOf<Int, CompletableDeferred<Any>>()

    @Serializable
    data class WebSocketConfig(
        val webSocketDebuggerUrl: String
    )

    init {
        val listener = PuppeteerWebSocketListener(channel)
        websocket = Request.Builder()
            .url("http://localhost:$port/json/version")
            .build().let {
                client.newCall(it)
                    .execute().use { response ->
                        response.body?.string()?.let { c ->
                            json.decodeFromString<WebSocketConfig>(c)
                                .webSocketDebuggerUrl.let { wsUrl ->
                                    Request.Builder().url(wsUrl).build().let { req ->
                                        client.newWebSocket(req, listener)
                                    }
                                }
                        }
                    }
            } ?: throw RobotException("Unable to connect to puppeteer")

        eventLoop = CompletableFuture.runAsync {
            runBlocking { startEventLoop() }
        }
    }

    private suspend fun startEventLoop() = coroutineScope {
        launch {
            while (true) {
                when (val msg = channel.receive()) {
                    PuppeteerWebSocketListener.PuppeteerMessage.CLOSING -> break
                    is PuppeteerWebSocketListener.PuppeteerResult -> {
                        producers.remove(msg.id)?.complete(msg.result)
                    }
                    is PuppeteerWebSocketListener.PuppeteerException -> {
                        producers.remove(msg.id)?.complete(msg)
                    }
                    else -> println("Receive $msg")
                }
            }
            release()
        }
    }

    private fun release() {
        channel.close()
        process.destroy()
        client.dispatcher.executorService.shutdownNow()
    }

    override fun close() {
        websocket.close(4001, reason = "CLOSING")
        eventLoop.get()
    }

    suspend fun <T> newTab(perform: suspend Tab.() -> T): T {
        val target = sendMessage("Target.createTarget", null, buildJsonObject {
            put("url", "about:blank")
        })
        val session = sendMessage("Target.attachToTarget", null, buildJsonObject {
            put("targetId", target["targetId"]!!)
            put("flatten", true)
        })
        return Tab(json.decodeFromJsonElement(
            buildJsonObject {
                target.forEach { k, v ->
                    put(k, v)
                }
                session.forEach { k, v ->
                    put(k, v)
                }
            }
        ), json, this::sendMessage).use {
            it.perform()
        }
    }

    private suspend fun sendMessage(method: String, sessionId: String?, payload: JsonObject) =
        coroutineScope {
            val id: Int = counter.incrementAndGet()
            val msg = buildJsonObject {
                put("id", id)
                put("method", method)
                put("params", json.encodeToJsonElement(payload))
                if (sessionId != null) put("sessionId", sessionId)
            }.toString()
            val result = CompletableDeferred<Any>()
            producers[id] = result
            websocket.send(msg)
            result.await().toJsonElement()
        }

    private fun Any.toJsonElement() =
        when (this) {
            is PuppeteerWebSocketListener.PuppeteerException -> throw RobotException(this.message.toString())
            is JsonObject -> this
            else -> throw RobotException("Not json object")
        }
}
