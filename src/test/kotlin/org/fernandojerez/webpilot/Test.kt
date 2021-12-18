package org.fernandojerez.webpilot

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.fernandojerez.webpilot.puppeteer.rpc.Puppeteer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestWebPilot {

    private val expectedContent = "<html><head></head><body><div>Hello World</div></body></html>"
    private val chromeExecutable = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"
    private val serverPort = 8080
    private val puppeteerPort = 33333

    private fun WebpilotConfig.assertContent() {
        val server = MockWebServer()
        val flag = AtomicBoolean()
        server.enqueue(MockResponse().setBody(expectedContent))
        server.start(serverPort)
        server.use {
            this.let {
                runBlocking {
                    Puppeteer().start(it).use { robot ->
                        robot.newTab {
                            navigate("http://localhost:$serverPort/tests") {
                                assertEquals(expectedContent, content())
                                flag.set(true)
                            }
                        }
                    }
                }
            }
        }
        assertTrue(flag.get(), "Content not asserted")
    }

    @Test
    fun testGetContentNoHeadless() {
        WebpilotConfig(
            headless = false,
            remotePort = puppeteerPort,
            chromeExecutable = chromeExecutable
        ).assertContent()
    }

    @Test
    fun testGetContentNoSandboxed() {
        WebpilotConfig(
            sandboxed = false,
            remotePort = puppeteerPort,
            chromeExecutable = chromeExecutable
        ).assertContent()
    }
}
