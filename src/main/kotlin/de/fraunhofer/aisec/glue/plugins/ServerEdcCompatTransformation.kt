/*
 * Copyright 2026 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.aisec.glue.plugins

import de.fraunhofer.aisec.glue.Constants
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
        on(ReceiveRequestBodyTransformed) { call, byteChannel ->
            if (call.shouldSkipEdcCompatTransformation() || byteChannel !is ByteReadChannel) {
                return@on byteChannel
            }
            ByteReadChannel(config.transformRequest(byteChannel.toByteArray()))
        }

        // Transform response BEFORE ContentNegotiation serialization
        on(SendResponseBodyBeforeContentNegotiation) { call, body ->
            if (call.shouldSkipEdcCompatTransformation() || body is OutgoingContent) {
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

private fun ApplicationCall.shouldSkipEdcCompatTransformation(): Boolean {
    val userAgent = request.userAgent() ?: return false
    return Constants.TC_USER_AGENT_REGEX.containsMatchIn(userAgent)
}
