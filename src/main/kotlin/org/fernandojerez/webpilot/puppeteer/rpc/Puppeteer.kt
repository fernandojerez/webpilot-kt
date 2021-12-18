package org.fernandojerez.webpilot.puppeteer.rpc

import org.fernandojerez.webpilot.WebpilotConfig
import org.fernandojerez.webpilot.puppeteer.api.Robot
import java.io.File

class Puppeteer {

    private fun isWindows(): Boolean = System.getProperty("os.name")!!.contains("windows")

    fun start(config: WebpilotConfig): Robot {
        val arguments = mutableListOf(
            config.chromeExecutable,
            "--remote-debugging-port=${config.port}"
        )
        if (config.headless) {
            arguments.add("--headless")
            if (isWindows()) arguments.add("--disable-gpu")
        }
        val dataDir = File(".webpilot_udata").let {
            it.mkdirs()
            it.absolutePath
        }
        arguments.add("--no-default-browser-check")
        arguments.add("--user-data-dir=$dataDir")
        if (config.sandboxed) arguments.add("--no-sandbox")
        return ProcessBuilder(arguments).start().let {
            Thread.sleep(2000)
            Robot(it, config.port)
        }
    }

}
