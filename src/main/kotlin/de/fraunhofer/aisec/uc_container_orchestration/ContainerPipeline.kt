package de.fraunhofer.aisec.uc_container_orchestration

import de.fraunhofer.aisec.uc_container_orchestration.docker_client.Container
import de.fraunhofer.aisec.uc_container_orchestration.docker_client.DockerClient
import de.fraunhofer.aisec.uc_container_orchestration.docker_client.Network
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.File
import java.net.InetAddress

class ContainerPipeline private constructor(
    private val client: DockerClient,
    private val ownsClient: Boolean,
    val selfContainer: Container?,
    val containers: Map<String, Container>,
    val networks: List<Network>
) : Closeable {
    override fun close() {
        if (ownsClient) {
            client.close()
        }
    }

    suspend fun start() {
        for (container in containers.values) {
            container.start()
        }
    }

    private suspend fun stopContainer(container: Container) {
        try {
            withTimeout(5000) {
                container.stop()
            }
        } catch (e: TimeoutCancellationException) {
            container.kill()
        }
    }

    suspend fun stop() {
        coroutineScope {
            for ((_, container) in containers) {
                launch {
                    stopContainer(container)
                }
            }
        }
    }

    suspend fun remove() {
        for (container in containers.values) {
            container.remove(deleteVolumes = true, force = true)
        }

        for (network in networks) {
            if (selfContainer != null) {
                network.disconnect(selfContainer)
            }
            network.remove()
        }
    }

    companion object {
        private fun getSelfContainer(client: DockerClient): Container? {
            // If we are running in a container, this file should exist
            return if (File("/.dockerenv").exists()) {
                client.containers.get(InetAddress.getLocalHost().hostName)
            } else {
                null
            }
        }

        suspend fun create(
            images: Sequence<Pair<String, String>>,
            dockerClient: DockerClient? = null
        ): ContainerPipeline {
            var ownsClient = false
            val client = if (dockerClient != null) {
                dockerClient
            } else {
                ownsClient = true
                DockerClient()
            }
            val selfContainer = getSelfContainer(client)

            val containers =
                images.asIterable()
                    .associate { (name, image) -> Pair(name, client.containers.create(name, image, init = true)) }
            val networks = images.asIterable().windowed(2).map { pair ->
                val (name1, _) = pair[0]
                val (name2, _) = pair[1]

                val link = Network.link(client, containers[name1]!!, containers[name2]!!)
                if (selfContainer != null) {
                    link.connect(selfContainer)
                }
                link
            }.toList()

            return ContainerPipeline(client, ownsClient, selfContainer, containers, networks)
        }
    }
}