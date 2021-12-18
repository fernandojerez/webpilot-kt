# Webpilot-kt

SImple API to get html content from pages

```kotlin
WebpilotConfig(
    sandboxed = false,
    remotePort = 33333,
    chromeExecutable = "<path-chrome>"
) let {
    runBlocking {
        Puppeteer().start(it).use { robot ->
            robot.newTab {
                navigate("http://localhost:8080/tests") {
                    assertEquals(expectedContent, content())
                    flag.set(true)
                }
            }
        }
    }
}
```
