package de.fraunhofer.aisec.glue.config

import io.ktor.http.*
import io.ktor.server.config.*
import java.net.InetAddress

/**
 * Configuration for the connector itself.
 */
class ConnectorConfig(inner: ApplicationConfig) : Config(inner) {
    /**
     * The identity of this connector that is used as assigner and assignee during contract negotiation.
     */
    val identity by required(stringProp("connector.identity"))

    /**
     * The default public URL to use for callbacks when starting a negotiation.
     * When this property is not set, the local IP address is used, which will likely not be routable in production.
     */
    val publicUrl by withDefault(convert(stringProp("connector.publicUrl"), ::Url), Url(InetAddress.getLocalHost().hostAddress))
}
