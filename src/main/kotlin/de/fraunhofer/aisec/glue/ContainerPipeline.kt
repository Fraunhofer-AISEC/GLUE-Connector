package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.docker.Container
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.docker.Network
import de.fraunhofer.aisec.glue.docker.model.ContainerCreateRequest
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
    val containers: List<Container>,
    val networks: List<Network>,
) : Closeable {
    override fun close() {
        if (ownsClient) {
            client.close()
        }
    }

    suspend fun start() {
        for (container in containers) {
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
            for (container in containers) {
                launch {
                    stopContainer(container)
                }
            }
        }
    }

    suspend fun remove() {
        for (container in containers) {
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
            dockerClient: DockerClient? = null,
        ): ContainerPipeline {
            var ownsClient = false
            val client =
                if (dockerClient != null) {
                    dockerClient
                } else {
                    ownsClient = true
                    DockerClient()
                }
            val selfContainer = getSelfContainer(client)

            val containers =
                images.asIterable()
                    .associate { (name, image) -> Pair(name, client.containers.create(name, image)) }
            val networks =
                images.asIterable().windowed(2).map { pair ->
                    val (name1, _) = pair[0]
                    val (name2, _) = pair[1]

                    val link = Network.link(client, containers[name1]!!, containers[name2]!!)
                    if (selfContainer != null) {
                        link.connect(selfContainer)
                    }
                    link
                }.toList()

            return ContainerPipeline(client, ownsClient, selfContainer, containers.values.toList(), networks)
        }

        suspend fun create(
            images: Iterable<Pair<String, ContainerCreateRequest>>,
            dockerClient: DockerClient? = null,
        ): ContainerPipeline {
            var ownsClient = false
            val client =
                if (dockerClient != null) {
                    dockerClient
                } else {
                    ownsClient = true
                    DockerClient()
                }
            val selfContainer = getSelfContainer(client)

            val containers = images.map { (name, spec) -> client.containers.create(name, spec) }
            val networks =
                containers.windowed(2).map { (c1, c2) ->
                    val link = Network.link(client, c1, c2)
                    if (selfContainer != null) {
                        link.connect(selfContainer)
                    }
                    link
                }

            return ContainerPipeline(client, ownsClient, selfContainer, containers, networks)
        }
    }
}
