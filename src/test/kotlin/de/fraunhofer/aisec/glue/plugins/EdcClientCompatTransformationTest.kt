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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.glue.Constants
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class EdcClientCompatTransformationTest {
    private val objectMapper = jacksonObjectMapper()

    data class Payload(
        val value: String,
    )

    private fun httpClient(mockFactory: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(mockFactory)) {
            install(ClientEdcCompatTransformation) {
                transformRequest = { body ->
                    when (body) {
                        is Payload -> body.copy(value = "transformed")
                        else -> body
                    }
                }
            }
            install(ClientContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper, streamBody = false))
            }
        }

    @Test
    fun `transforms request body object before it is sent`() =
        runTest {
            val client =
                httpClient { request ->
                    val payload = objectMapper.readValue(request.body.toByteArray(), Payload::class.java)
                    assertEquals("transformed", payload.value)

                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            client.post("https://example.com/test") {
                contentType(ContentType.Application.Json)
                setBody(Payload("original"))
                attributes.put(EdcCompatPeerEndpointAttribute, "https://example.com")
            }

            client.close()
        }

    @Test
    fun `skips transformation for the tc server identifier`() =
        runTest {
            val client =
                httpClient { request ->
                    if (request.method == HttpMethod.Head) {
                        return@httpClient respond(
                            content = "",
                            status = HttpStatusCode.NoContent,
                            headers =
                                headers {
                                    append(HttpHeaders.Server, Constants.TC_USER_AGENT)
                                },
                        )
                    }

                    val payload = objectMapper.readValue(request.body.toByteArray(), Payload::class.java)
                    assertEquals("original", payload.value)

                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            client.post("https://example.com/test") {
                contentType(ContentType.Application.Json)
                setBody(Payload("original"))
                attributes.put(EdcCompatPeerEndpointAttribute, "https://example.com")
            }

            client.close()
        }
}
