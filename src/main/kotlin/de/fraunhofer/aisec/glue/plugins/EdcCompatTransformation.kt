package de.fraunhofer.aisec.glue.plugins

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.utils.io.*

class EdcCompatTransformationConfig {
    var transformRequest: (String) -> String = { it }
    var transformResponse: (String) -> String = { it }
}

val EdcCompatTransformation =
    createRouteScopedPlugin(name = "EdcCompatTransformation", ::EdcCompatTransformationConfig) {
        val config = pluginConfig

        // Phase ordering: this runs BEFORE ContentNegotiation
        on(ReceiveRequestBodyTransformed) { _, body ->
            if (body !is ByteReadChannel) return@on body

            val originalText = body.readRemaining().readText()
            val transformedText = config.transformRequest(originalText)

            // Return a new channel with transformed content
            ByteReadChannel(transformedText)
        }

        // Transform response AFTER serialization
        onCallRespond { _, body ->
            if (body !is OutgoingContent) return@onCallRespond

            transformBody { content ->
                when (content) {
                    is TextContent -> {
                        val transformed = config.transformResponse(content.text)
                        TextContent(transformed, content.contentType, content.status)
                    }
                    else -> content
                }
            }
        }
    }

// Hook definition that runs before ContentNegotiation deserializes
private object ReceiveRequestBodyTransformed : Hook<suspend (ApplicationCall, Any) -> Any> {
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall, Any) -> Any,
    ) {
        pipeline.receivePipeline.intercept(ApplicationReceivePipeline.Before) {
            val transformed = handler(call, subject)
            proceedWith(transformed)
        }
    }
}
