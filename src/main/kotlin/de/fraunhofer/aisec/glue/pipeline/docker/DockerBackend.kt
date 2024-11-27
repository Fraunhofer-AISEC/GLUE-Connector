package de.fraunhofer.aisec.glue.pipeline.docker

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.docker.Container
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.docker.InetUtil
import de.fraunhofer.aisec.glue.docker.Network
import de.fraunhofer.aisec.glue.docker.model.ContainerCreateRequest
import de.fraunhofer.aisec.glue.docker.model.HostConfig
import de.fraunhofer.aisec.glue.docker.model.PortBinding
import de.fraunhofer.aisec.glue.pipeline.Backend
import de.fraunhofer.aisec.glue.pipeline.ContainerInfo
import de.fraunhofer.aisec.glue.pipeline.ContainerPipeline
import fhg.glue.odrl.GlueContainerPipeline

class DockerBackend(
    private val objectMapper: ObjectMapper,
    private val dockerClient: DockerClient = createDockerClient()
) : Backend {
    private val subnetLen = 29
    private val subnets =
        InetUtil.getSubnets(InetUtil.parseInet4Address("10.1.0.0"), 16, subnetLen)
            .map { a -> "${a.hostAddress}/$subnetLen" }
            .iterator()

    private fun convertPipeline(
        id: String,
        pipelineDef: GlueContainerPipeline,
    ): MutableList<ContainerCreateRequest> {
        return pipelineDef.containers.map { c ->
            // TODO: Provide configurable defaults for other parameters
            //       We probably should persist this to the point where we inject the config
            ContainerCreateRequest(
                image = c.image,
                hostConfig =
                    HostConfig(
                        // TODO: In case allowNetwork is true, use an isolated bridge instead of the default bridge
                        networkMode = if (c.allowNetwork) "bridge" else "none",
                    ),
                // Attach labels to the containers so we can recover running pipelines if we crash and/or restart
                labels =
                    mapOf(
                        "de.fraunhofer.aisec.glue.pipeline.id" to id,
                        "de.fraunhofer.aisec.glue.pipeline.contract" to "TODO: Put contract ID here",
                        "de.fraunhofer.aisec.glue.pipeline.owner" to "TODO: Put identity of whoever spawned the pipeline here",
                    ),
            )
        }.toMutableList()
    }

    private suspend fun createContainers(
        id: String,
        containerSpecs: List<ContainerCreateRequest>,
    ): List<Container> {
        val containers = mutableListOf<Container>()

        try {
            // We need to add the containers one by one so all existing containers are freed correctly on exception
            for ((i, cs) in containerSpecs.withIndex()) {
                val name = "pipeline-$id-$i"
                val container = dockerClient.containers.create(name, cs)
                containers.add(container)
            }
        } catch (e: Exception) {
            containers.forEach { c -> c.runCatching { remove() } }
            throw e
        }

        return containers
    }

    private suspend fun linkContainers(
        id: String,
        containers: List<Container>,
    ): List<Network> {
        val containerPairs = containers.windowed(2)
        val networks = mutableListOf<Network>()

        try {
            for ((i, c) in containerPairs.withIndex()) {
                val name = "pipeline-$id-net-$i"
                // TODO: Instead of arena allocating subnets, make sure to also reissue released subnets if available
                val subnet = subnets.next()
                val network = dockerClient.networks.create(name, subnet)
                networks.add(network)
                network.connect(c[0])
                network.connect(c[1])
            }
        } catch (e: Exception) {
            networks.forEach { n -> n.runCatching { remove() } }
            throw e
        }

        return networks
    }

    override suspend fun createPipeline(
        id: String,
        policyObject: GlueContainerPipeline,
    ): ContainerPipeline {
        // TODO: Pull images and verify digests here
        //       Feel free to extract that into a different function or class

        val containerCreateReqs = convertPipeline(id, policyObject)

        if (containerCreateReqs.isEmpty()) {
            throw IllegalArgumentException("The provided spec contains no containers")
        }

        // TODO: Read image labels to determine ports to expose
        //       Alternatively, we can simply expose all ports that have a corresponding EXPOSE in the dockerfile.
        //       While simpler, that could have unintended side effects and lead to over exposure
        val publishPorts = listOf("8080/tcp")

        containerCreateReqs[0] =
            containerCreateReqs[0].copy(
                exposedPorts = publishPorts.associateWith { emptyMap<Any, Any>() },
                hostConfig =
                    containerCreateReqs[0].hostConfig!!.copy(
                        portBindings =
                            publishPorts.associateWith {
                                listOf(
                                    PortBinding(
                                        // TODO: Allow a configured external IP address?
                                        hostPort = "",
                                    ),
                                )
                            },
                    ),
            )

        val containers = createContainers(id, containerCreateReqs)
        val networks =
            try {
                linkContainers(id, containers)
            } catch (e: Exception) {
                containers.forEach { c -> c.runCatching { remove() } }
                throw e
            }

        return DockerContainerPipeline(id, objectMapper, containers, networks)
    }

    override suspend fun getConnectorIp(relativeTo: ContainerInfo): String {
        TODO("Not yet implemented")
    }

    companion object {
        fun createDockerClient(): DockerClient {
            val socketPath = System.getenv("DOCKER_SOCKET") ?: "/var/run/docker.sock"
            return DockerClient(socketPath)
        }
    }
}
