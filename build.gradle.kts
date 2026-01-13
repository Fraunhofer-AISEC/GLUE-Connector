import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegen
import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegenPlugin
import net.pwall.json.kotlin.codegen.gradle.mapping.ClassMappingByURI
import java.net.URI

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
    alias(libs.plugins.jib)
    alias(libs.plugins.shadow)
    application
}

group = "fhg.glue.container-pipeline"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.jsonKotlinSchemaCodegen)
    }
}

apply<JSONSchemaCodegenPlugin>()

kotlin {
    jvmToolchain(25)
    sourceSets {
        main {
            kotlin.srcDir("${layout.buildDirectory.get()}/generated-sources/kotlin")
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
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
    implementation("fhg.glue.odrl:odrl-parser:1.1-SNAPSHOT")
    implementation(libs.duckdb)
    implementation(libs.ktorm.core)
    implementation(libs.ktorm.jackson)
    implementation(libs.dbcp)
    implementation(libs.kotlinLogging.jvm)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.duckdb)
    // For running example with gradle
    runtimeOnly(libs.slf4j.jdk14)

    testImplementation(libs.slf4j.jdk14)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.bytebuddy)
}

tasks.jar {
    manifest {

    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dkotlinx.coroutines.debug", "-Dkotlinx.coroutines.stacktrace.recovery=true")
}


application {
    mainClass.set("de.fraunhofer.aisec.glue.ApplicationKt")
}

jib {
    from {
        image = "eclipse-temurin:25-jre"
    }
    container {
        mainClass = "de.fraunhofer.aisec.glue.ApplicationKt"
        // Activates Kotlin's debug mode inside the container
        jvmFlags = listOf("-ea")
    }
}

// Required for ./gradlew run in order to wait on readln()
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

// A tool to simply register a policy with the connector
tasks.register<JavaExec>("runPolicyTool") {
    group = "tools"
    description = "Run a tool to register a policy with a running connector"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("de.fraunhofer.aisec.glue.tools.RegisterPolicyKt")
    standardInput = System.`in`
}

configure<JSONSchemaCodegen> {
    // TODO: If we ever want to generate non-cmc json schemata, we need to find a more flexible solution
    packageName.set("de.fraunhofer.aisec.glue.atls")
    classMappings {
        fun makeMetadataMapAlias(className: String): ClassMappingByURI {
            return byURI(
                "de.fraunhofer.aisec.glue.atls.MetadataMap",
                URI("file", File("src/main/resources/schema/$className.json").absoluteFile.toString(), "/\$defs/$className/properties/metadata")
            )
        }

        makeMetadataMapAlias("AtlsHandshakeResponse")
        makeMetadataMapAlias("AttestationResponse")
        makeMetadataMapAlias("VerificationRequest")
    }
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
