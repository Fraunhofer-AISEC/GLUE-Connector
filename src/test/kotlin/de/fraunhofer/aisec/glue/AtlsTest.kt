package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.glue.atls.CMCClient
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import kotlin.test.Test
import kotlin.test.assertTrue

class AtlsTest {
    @Test
    @DisabledIfEnvironmentVariable(named = "GITLAB_CI", matches = "*")
    fun `Test functionality of the CMC API`() =
        runTest {
            val client = CMCClient(InetSocketAddress("127.0.0.1", 9955), jacksonObjectMapper())

            val report = client.attest("This is a test".toByteArray())
            val verify = client.verify("This is a test".toByteArray(), report)
            assertTrue(verify)
        }
}
