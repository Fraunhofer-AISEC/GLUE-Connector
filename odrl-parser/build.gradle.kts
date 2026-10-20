plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.titaniumJsonLd)
    implementation(libs.jakarta.json.api)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsonp)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.konform)

    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.parsson)
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
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
        licenseHeader(
            "/*\n" +
            " * Copyright \$YEAR Fraunhofer AISEC and Fraunhofer IOSB\n" +
            " *\n" +
            " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
            " * you may not use this file except in compliance with the License.\n" +
            " * You may obtain a copy of the License at\n" +
            " *\n" +
            " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
            " *\n" +
            " * Unless required by applicable law or agreed to in writing, software\n" +
            " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            " * See the License for the specific language governing permissions and\n" +
            " * limitations under the License.\n" +
            " */"
        )
    }
}
