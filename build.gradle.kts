plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "de.fraunhofer.aisec"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.10")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // For running example with gradle
    runtimeOnly("org.slf4j:slf4j-jdk14:2.0.10")

    testImplementation("org.slf4j:slf4j-jdk14:2.0.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("de.fraunhofer.aisec.glue.ExampleKt")
}

// Required for ./gradlew run in order to wait on readln()
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

spotless {
    kotlin {
        target("src/*/kotlin/**/*.kt")
        ktlint().editorConfigOverride(mapOf(
            "ktlint_code_style" to "ktlint_official",
            // Since intellij generates inputs like this, this rule is in the way...
            "ktlint_standard_no-wildcard-imports" to "disabled",
        ))
    }
}