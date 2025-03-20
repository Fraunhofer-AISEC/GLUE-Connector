package de.fraunhofer.aisec.glue.docker

import de.fraunhofer.aisec.glue.Utils.hexEncode
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test

class DockerClientTest {
    private val client = DockerClient.fromEnv()

    @Test
    fun testPing() {
        runBlocking {
            client.ping()
        }
    }

    @Test
    fun testContainerFlow() {
        runBlocking {
            val name = Random.Default.nextBytes(10).hexEncode()
            val httpEchoImage = System.getenv("HTTP_ECHO_IMAGE_NAME") ?: "container-pipeline/http-echo"
            val container = client.containers.create(name, httpEchoImage)
            try {
                container.start()
                container.stop()
            } finally {
                container.remove()
            }
        }
    }

    @Test
    fun testNetworkFlow() {
        runBlocking {
            val name = Random.Default.nextBytes(10).hexEncode()
            val net = client.networks.create(name, "10.0.${Random.Default.nextInt(256)}.0/24")
            net.remove()
        }
    }
}
