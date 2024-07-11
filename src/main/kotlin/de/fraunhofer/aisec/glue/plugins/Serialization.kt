package de.fraunhofer.aisec.glue.plugins

import com.apicatalog.jsonld.JsonLdOptions
import de.fraunhofer.aisec.glue.jsonld.JsonLDModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            registerModules(JsonLDModule(JsonLdOptions()))
        }
    }
}
