plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.shadow)
    alias(libs.plugins.spotless)
    application
}

group = "de.fraunhofer.aisec"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.auth)
    implementation(libs.koin.ktor)

    // For running example with gradle
    runtimeOnly(libs.slf4j.jdk14)

    testImplementation(libs.slf4j.jdk14)
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("de.fraunhofer.aisec.glue.ApplicationKt")
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