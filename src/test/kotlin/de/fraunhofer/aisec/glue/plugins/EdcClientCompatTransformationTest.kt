package de.fraunhofer.aisec.glue.plugins

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

    data class Payload(val value: String)

    @Test
    fun `transforms request body object before it is sent`() =
        runTest {
            val client =
                HttpClient(
                    MockEngine { request ->
                        val payload = objectMapper.readValue(request.body.toByteArray(), Payload::class.java)
                        assertEquals("transformed", payload.value)

                        respond(
                            content = "",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                ) {
                    install(ClientEdcCompatTransformation) {
                        transformRequest = { body ->
                            when (body) {
                                is Payload -> body.copy(value = "transformed")
                                else -> body
                            }
                        }
                    }
                    install(ClientContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper, streamRequestBody = false))
                    }
                }

            client.post("https://example.com/test") {
                contentType(ContentType.Application.Json)
                setBody(Payload("original"))
            }

            client.close()
        }
}
