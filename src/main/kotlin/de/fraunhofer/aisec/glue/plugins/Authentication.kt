package de.fraunhofer.aisec.glue.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuthentication() {
    install(Authentication) {
        // TODO: Replace with proper JWT or OAuth authentication at some point
        basic("ingress-auth") {
            realm = "Ingress"
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
