package de.fraunhofer.aisec.glue.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuthentication() {
    install(Authentication) {
        basic {
            realm = "Test auth todo replace"
            skipWhen { true } // Skip authentication entirely
            validate { creds ->
                if (creds.name == "user" && creds.password == "password") {
                    UserIdPrincipal(creds.name)
                } else {
                    null
                }
            }
        }
    }
}
