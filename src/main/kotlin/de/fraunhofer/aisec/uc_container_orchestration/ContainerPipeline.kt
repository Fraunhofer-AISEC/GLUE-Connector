package de.fraunhofer.aisec.uc_container_orchestration

import de.fraunhofer.aisec.uc_container_orchestration.docker_client.Container
import de.fraunhofer.aisec.uc_container_orchestration.docker_client.DockerClient
import de.fraunhofer.aisec.uc_container_orchestration.docker_client.Network
import java.io.File
import java.net.InetAddress

class ContainerPipeline private constructor(val selfContainer: Container?, val containers: Map<String, Container>, val networks: List<Network>) {
    suspend fun start() {
        for (container in containers.values) {
            container.start()
        }
    }

    suspend fun stop() {
        for (container in containers.values) {
            container.stop()
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

        suspend fun create(images: Sequence<Pair<String, String>>): ContainerPipeline {
            val client = DockerClient()
            val selfContainer = getSelfContainer(client)

            val containers =
                images.asIterable().associate { (name, image) -> Pair(name, client.containers.create(name, image, init = true)) }
            val networks = images.asIterable().windowed(2).map { pair ->
                val (name1, _) = pair[0]
                val (name2, _) = pair[1]

                val link = Network.link(client, containers[name1]!!, containers[name2]!!)
                if (selfContainer != null) {
                    link.connect(selfContainer)
                }
                link
            }.toList()

            return ContainerPipeline(selfContainer, containers, networks)
        }
    }
}