package de.fraunhofer.aisec.glue.plugins

import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.http.content.*

class ClientEdcCompatTransformationConfig {
    var transformRequest: (Any) -> Any = { it }
}

val ClientEdcCompatTransformation =
    createClientPlugin(name = "ClientBodyTransformation", ::ClientEdcCompatTransformationConfig) {
        val config = pluginConfig

        on(TransformRequestBodyObject) { _, body ->
            config.transformRequest(body)
        }
    }

private object TransformRequestBodyObject : ClientHook<suspend (HttpRequestBuilder, Any) -> Any> {
    override fun install(
        client: HttpClient,
        handler: suspend (HttpRequestBuilder, Any) -> Any,
    ) {
        client.requestPipeline.intercept(HttpRequestPipeline.Transform) {
            if (subject is OutgoingContent) {
                return@intercept
            }

            val transformed = handler(context, subject)
            proceedWith(transformed)
        }
    }
}
