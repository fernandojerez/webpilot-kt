package org.fernandojerez.webpilot

import java.net.ServerSocket

data class WebpilotConfig(
    val remotePort: Int? = null,
    val url: String = "about:blank",
    val headless: Boolean = true,
    val sandboxed: Boolean = true,
    val chromeExecutable: String = "chrome"
) {
    val port: Int
        get() = remotePort ?: kotlin.run {
            val server = ServerSocket(0)
            val port = server.localPort
            server.close()
            port
        }
}
