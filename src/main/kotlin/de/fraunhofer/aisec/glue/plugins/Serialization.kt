package de.fraunhofer.aisec.glue.plugins

import de.fraunhofer.aisec.glue.jsonld.KtorJsonLDConverter.Companion.jsonLd
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson()
        jsonLd()
    }
}
