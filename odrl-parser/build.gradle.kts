plugins {
    alias(libs.plugins.kotlin)
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
