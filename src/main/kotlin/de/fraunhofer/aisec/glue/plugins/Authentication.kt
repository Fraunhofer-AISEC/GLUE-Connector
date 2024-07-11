package de.fraunhofer.aisec.glue.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuthentication() {
    install(Authentication) {
        basic {
            realm = "Test auth todo replace"
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
