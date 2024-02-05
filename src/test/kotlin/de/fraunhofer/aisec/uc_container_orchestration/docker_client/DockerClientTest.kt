package de.fraunhofer.aisec.uc_container_orchestration.docker_client

import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test

class DockerClientTest {
    private val client = DockerClient(DOCKER_SOCKET)

    @Test
    fun testPing() {
        runBlocking {
            client.ping()
        }
    }

    @Test
    fun testContainerFlow() {
        runBlocking {
            val name = Random.Default.nextBytes(10).joinToString { b -> "%02x".format(b) }
            val container = client.containers.create(name, "test-container:latest")
            container.start()
            container.stop()
            container.remove()
        }
    }

    @Test
    fun testNetworkFlow() {
        runBlocking {
            val name = Random.Default.nextBytes(10).joinToString { b -> "%02x".format(b) }
            val net = client.networks.create(name, "10.0.${Random.Default.nextInt(256)}.0/24")
            net.remove()
        }
    }

    companion object {
        const val DOCKER_SOCKET = "/var/run/docker.sock"

        // Proxy socket to dump requests and responses
        // $ socat -v UNIX-LISTEN:/run/user/1000/docker-socket.sock,fork UNIX-CONNECT:/var/run/docker.sock
        //const val DOCKER_SOCKET = "/run/user/1000/docker-socket.sock"
    }
}