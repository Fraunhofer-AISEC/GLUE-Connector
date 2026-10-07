import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegen
import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegenPlugin
import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegenTask
import net.pwall.json.kotlin.codegen.gradle.mapping.ClassMappingByURI
import java.net.URI

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
    alias(libs.plugins.jib)
    alias(libs.plugins.shadow)
    alias(libs.plugins.cyclonedx)
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
    // Logback for SLF4J implementation
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
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
    implementation(project(":odrl-parser"))
    implementation(libs.duckdb)
    implementation(libs.ktorm.core)
    implementation(libs.ktorm.jackson)
    implementation(libs.dbcp)
    implementation(libs.kotlinLogging.jvm)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.duckdb)
    // Parsson is the default implementation for jakarta.json-api 2.1.3
    runtimeOnly(libs.parsson)

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
        image = "eclipse-temurin:25-jre@sha256:681c543d6f36c50f45e9b5226930a46203dcfa351d3670e9d0bdf0dabae53539"
        platforms {
            platform {
                os = "linux"
                architecture = "amd64"
            }
            platform {
                os = "linux"
                architecture = "arm64"
            }
        }
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

tasks.withType<JSONSchemaCodegenTask>().configureEach {
    val schemaFiles = fileTree("src/main/resources/schema") {
        include("**/*.json")
    }
    inputs.files(schemaFiles)
        .withPropertyName("schemaFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(layout.buildDirectory.dir("generated-sources/kotlin"))
}

tasks {
    shadowJar {
        mergeServiceFiles()
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
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
