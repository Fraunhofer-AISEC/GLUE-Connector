package de.fraunhofer.aisec.glue.pipeline.docker

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.config.Config
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.docker.Container
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.docker.InetUtil
import de.fraunhofer.aisec.glue.docker.Network
import de.fraunhofer.aisec.glue.docker.model.*
import de.fraunhofer.aisec.glue.pipeline.AugmentedGlueContainer
import de.fraunhofer.aisec.glue.pipeline.Backend
import de.fraunhofer.aisec.glue.pipeline.ContainerInfo
import de.fraunhofer.aisec.glue.pipeline.ContainerPipeline
import de.fraunhofer.aisec.glue.pipeline.atls.AtlsConfig
import io.ktor.server.config.ApplicationConfig
import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.experimental.or

class DockerBackend(
    private val objectMapper: ObjectMapper,
    private val dockerClient: DockerClient,
    configManager: ConfigManager,
) : Backend {
    private val dockerSubnet = System.getenv("DOCKER_SUBNET") ?: "10.1.0.0"
    private val subnetLen = 29
    private val subnets =
        InetUtil.getSubnets(InetUtil.parseInet4Address(dockerSubnet), 16, subnetLen)
            .iterator()
    private val config = configManager.getConfig(::DockerConfig)
    private val atlsConfig = configManager.getConfig(::AtlsConfig)

    // Note: This is an implementation detail of docker that should not be relied upon.
    // Unfortunately, we don't have much of a choice.
    private val selfContainer: Container? by lazy {
        null
        /*if (File("/.dockerenv").exists()) {
            // Docker
            // This normally works fine, but can fail if the container has a different hostname set.
            // It may be better to fall back to get the PTR records for the local IP as one of the names is always the
            // container id (unless of course a different dns is used, but then the whole networking of this system will
            // fail anyway).
            Container(dockerClient, InetAddress.getLocalHost().hostName)
        } else if (File("/run/.containerenv").exists()) {
            // Podman
            Container(dockerClient, InetAddress.getLocalHost().hostName)
        } else {
            // No container
            null
        }*/
    }

    private suspend fun convertPipeline(
        id: String,
        pipelineSpec: List<AugmentedGlueContainer>,
        contractId: String,
        targetId: String,
    ): MutableList<ContainerCreateRequest> {
        checkHashes(pipelineSpec)
        return pipelineSpec.map { c ->
            //       We probably should persist this to the point where we inject the config
            ContainerCreateRequest(
                image = c.base.image,
                hostConfig =
                    HostConfig(
                        networkMode = if (c.base.allowNetwork == true) "bridge" else "none",
                        binds = c.extraVolumes.map { (hostPath, containerPath) -> "$hostPath:$containerPath" },
                    ),
                volumes = c.extraVolumes.map { (_, containerPath) -> containerPath to object {} }.toMap(),
                // Attach labels to the containers so we can recover running pipelines if we crash and/or restart
                labels =
                    mapOf(
                        "de.fraunhofer.aisec.glue.pipeline.id" to id,
                        "de.fraunhofer.aisec.glue.pipeline.contract" to contractId,
                        "de.fraunhofer.aisec.glue.pipeline.target" to targetId,
                        "de.fraunhofer.aisec.glue.pipeline.owner" to "TODO: Put identity of whoever spawned the pipeline here",
                    ),
            )
        }.toMutableList()
    }

    // Hash check adapted from https://github.com/Fraunhofer-AISEC/trusted-connector/blob/16fb97953c89e796d80c52cacc7a4445c1dda019/ids-dataflow-control/src/main/kotlin/de/fhg/aisec/ids/dataflowcontrol/usagecontrol/DockerImageConstraint.kt#L104
    private suspend fun checkHashes(pipelineDef: List<AugmentedGlueContainer>): Boolean {
        for (glueContainer in pipelineDef) {
            val expectedHash = glueContainer.base.hash.split(":").last()
            val imageToCheck = dockerClient.images.get(glueContainer.base.image)
            val inspectResponse = imageToCheck.inspect()
            val idHash = inspectResponse.id?.split(":")?.last()

            if (expectedHash == idHash ||
                inspectResponse.repoDigests?.any { expectedHash == it.split(":").last() } == true
            ) {
                continue
            }

            throw Exception(
                "The hash check for container ${glueContainer.base.image} failed. " +
                    "Expected $expectedHash, found candidates:\nID Hash: $idHash, " +
                    "Repo Digests: ${inspectResponse.repoDigests?.joinToString(", ")}",
            )
        }
        return true
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
        contractId: String,
        targetId: String,
    ): List<Network> {
        val containerPairs = containers.windowed(2)
        val networks = mutableListOf<Network>()

        try {
            for ((i, c) in containerPairs.withIndex()) {
                val name = "pipeline-$id-net-$i"
                val subnet = subnets.next()
                val subnetCidr = "${subnet.hostAddress}/$subnetLen"
                val gateway = Inet4Address.getByAddress(subnet.address.also { a -> a[3] = a[3] or 1 }).hostAddress
                val network =
                    dockerClient.networks.create(
                        NetworkCreateRequest(
                            name = name,
                            ipam =
                                IPAM(
                                    config =
                                        listOf(
                                            IPAMConfig(
                                                subnet = subnetCidr,
                                                gateway = gateway,
                                            ),
                                        ),
                                ),
                            labels =
                                mapOf(
                                    "de.fraunhofer.aisec.glue.pipeline.id" to id,
                                    "de.fraunhofer.aisec.glue.pipeline.contract" to contractId,
                                    "de.fraunhofer.aisec.glue.pipeline.target" to targetId,
                                ),
                        ),
                    )

                networks.add(network)
                network.connect(c[0])
                network.connect(c[1])
                // We always want to have the controller reachable
                selfContainer?.let { self -> network.connect(self) }
            }
        } catch (e: Exception) {
            networks.forEach { n -> n.runCatching { remove() } }
            throw e
        }

        return networks
    }

    /*private suspend fun connectController(id: String, containers: List<Container>, networks: List<Network>) {
        if (selfContainer != null) {
            // When we are running in a container, we just need to connect up to the link networks.
            // In the special case where there is only one container, we don't have a link network and need to fall back
            // to the non-dockerized behaviour.
            networks.forEach { net -> net.connect(selfContainer!!) }
            if ()
        }
    }*/

    override suspend fun createPipeline(
        id: String,
        pipelineSpec: List<AugmentedGlueContainer>,
        contractId: String,
        targetId: String,
    ): ContainerPipeline {
        val containerCreateReqs = convertPipeline(id, pipelineSpec, contractId, targetId)

        if (containerCreateReqs.isEmpty()) {
            throw IllegalArgumentException("The provided spec contains no containers")
        }

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
                                        hostPort = "",
                                    ),
                                )
                            },
                    ),
            )

        val containers = createContainers(id, containerCreateReqs)
        val networks =
            try {
                linkContainers(id, containers, contractId, targetId)
            } catch (e: Exception) {
                containers.forEach { c -> c.runCatching { remove() } }
                throw e
            }

        return DockerContainerPipeline(id, contractId, targetId, objectMapper, containers, networks, config)
    }

    override suspend fun getConnectorIp(relativeTo: ContainerInfo): String {
        val container = dockerClient.containers.get(relativeTo.name).inspect()
        // If the container is in no network, then this will be null
        val containerNetworks =
            container.networkSettings?.networks
                ?: throw IllegalStateException("Container ${relativeTo.name} is in no networks")
        val address =
            if (selfContainer != null) {
                val selfInspect = selfContainer!!.inspect()
                // Note: This may be O(n^2), but containerNetworks has length at most 2, so 2 calls for every network
                val sharedNetworks = selfInspect.networkSettings?.networks?.filter { (k, _) -> k in containerNetworks }
                // Return the ip address of the self container in the shared network
                sharedNetworks?.values?.firstOrNull()?.ipAddress
            } else {
                // The docker network properties are unfortunately only initialized after the container has started.
                containerNetworks.keys.firstOrNull()?.let { network ->
                    dockerClient.networks.get(network).inspect().ipam?.config?.get(0)?.gateway
                }
            }
        return address ?: throw IllegalStateException("No shared address exists with container ${relativeTo.name}")
    }

    override suspend fun getPipelineFromIp(ipAddress: String): String? {
        // Unfortunately, due to the highly nullable (defaultable?) nature of Go, we have to do a bunch of null checks
        // on every property...
        return dockerClient.containers.list()
            .filter { c -> c.networkSettings?.networks?.values?.any { n -> n.ipAddress == ipAddress } ?: false }
            .firstNotNullOfOrNull { c -> c.labels?.get("de.fraunhofer.aisec.glue.pipeline.id") }
    }

    class DockerConfig(inner: ApplicationConfig) : Config(inner) {
        val publicHostname by withDefault(stringProp(), InetAddress.getLocalHost().hostAddress)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DockerBackend::class.java)
    }
}
