package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.glue.atls.AtlsSocketFactory
import de.fraunhofer.aisec.glue.atls.CMCClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.test.Test
import kotlin.test.assertTrue

class AtlsTest {
    @Test
    @DisabledIfEnvironmentVariable(named = "GITLAB_CI", matches = ".*")
    fun `Test functionality of the CMC API`() =
        runTest {
            val client = CMCClient(InetSocketAddress("127.0.0.1", 9955), jacksonObjectMapper())

            val report = client.attest("This is a test".toByteArray())
            val verify = client.verify("This is a test".toByteArray(), report.report, report.metadata)
            assertTrue(verify)
        }

    @Test
    @DisabledIfEnvironmentVariable(named = "GITLAB_CI", matches = ".*")
    fun `Test ATLS client-side handshake with external server`() {
        val dispatcher = Dispatchers.IO
        val scope = CoroutineScope(dispatcher)
        val objectMapper = jacksonObjectMapper()

        val cmcClient = CMCClient(InetSocketAddress("127.0.0.1", 9955), objectMapper)
        val tlsContext = SSLContext.getInstance("TLS")
        tlsContext.init(null, arrayOf(TrustingX509TrustManager), SecureRandom())
        val httpClient =
            HttpClient(OkHttp) {
                engine {
                    config {
                        sslSocketFactory(
                            AtlsSocketFactory(objectMapper, cmcClient, dispatcher, tlsContext.socketFactory),
                            TrustingX509TrustManager,
                        )
                    }
                }
            }

        val resp =
            runBlocking(scope.coroutineContext) {
                httpClient.get("https://localhost:4443/get")
            }
        assert(resp.status.isSuccess())
    }

    object TrustingX509TrustManager : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<out X509Certificate?>?,
            authType: String?,
        ) {
            // NOOP
        }

        override fun checkServerTrusted(
            chain: Array<out X509Certificate?>?,
            authType: String?,
        ) {
            // NOOP
        }

        override fun getAcceptedIssuers(): Array<out X509Certificate?> = emptyArray()
    }
}
