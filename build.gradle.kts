plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    jacoco
}

val okhttpVersion = "4.9.3"

group = "fernandojerez"
version = "0.1.0"

System.getenv("FF_BUILD_DIR")?.run {
    project.buildDir = File("$this/opensource/${project.name}")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC3")

    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
}

tasks {
    test {
        useJUnitPlatform()
        configure<JacocoTaskExtension> {
            isEnabled = true
            setDestinationFile(layout.buildDirectory.file("jacoco/${name}.exec").get().asFile)
        }
    }
    withType<JacocoReport> {
        reports.apply {
            xml.required.set(false)
            csv.required.set(false)
            html.outputLocation.set(layout.buildDirectory.dir("reports/jacocoHtml"))
        }
    }
}

extensions.configure(JacocoPluginExtension::class) {
    version = "0.8.7"
    reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}
