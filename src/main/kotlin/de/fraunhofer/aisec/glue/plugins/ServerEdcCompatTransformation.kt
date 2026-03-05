package de.fraunhofer.aisec.glue.plugins

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*

class ServerEdcCompatTransformationConfig {
    var transformRequest: (ByteArray) -> ByteArray = { it }
    var transformResponse: (Any) -> Any = { it }
}

val ServerEdcCompatTransformation =
    createRouteScopedPlugin(name = "EdcCompatTransformation", ::ServerEdcCompatTransformationConfig) {
        val config = pluginConfig

        // Phase ordering: this runs BEFORE ContentNegotiation
        on(ReceiveRequestBodyTransformed) { _, byteChannel ->
            if (byteChannel !is ByteReadChannel) {
                return@on byteChannel
            }
            ByteReadChannel(config.transformRequest(byteChannel.toByteArray()))
        }

        // Transform response BEFORE ContentNegotiation serialization
        on(SendResponseBodyBeforeContentNegotiation) { _, body ->
            if (body is OutgoingContent) {
                return@on body
            }
            config.transformResponse(body)
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

// Hook definition that runs before ContentNegotiation serializes the response body
val beforeTransform = PipelinePhase("BeforeTransform")
private object SendResponseBodyBeforeContentNegotiation : Hook<suspend (ApplicationCall, Any) -> Any> {
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall, Any) -> Any,
    ) {
        pipeline.sendPipeline.insertPhaseBefore(ApplicationSendPipeline.Transform, beforeTransform)
        pipeline.sendPipeline.intercept(beforeTransform) {
            val transformed = handler(call, subject)
            proceedWith(transformed)
        }
    }
}
