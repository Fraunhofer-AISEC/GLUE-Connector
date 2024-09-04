plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.shadow)
    alias(libs.plugins.spotless)
    alias(libs.plugins.jib)
    application
}

group = "fhg.glue.container-pipeline"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.auth)
    implementation(libs.kodein)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsonp)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.titaniumJsonLd)
    implementation(libs.jakarta.json)
    implementation("fhg.glue.odrl:odrl-parser:1.0-SNAPSHOT")

    // For running example with gradle
    runtimeOnly(libs.slf4j.jdk14)

    testImplementation(libs.slf4j.jdk14)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("de.fraunhofer.aisec.glue.ApplicationKt")
}

jib {
    container.mainClass = "de.fraunhofer.aisec.glue.ApplicationKt"
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
            "ktlint_standard_if-else-wrapping" to "disabled",
        ))
    }
}
