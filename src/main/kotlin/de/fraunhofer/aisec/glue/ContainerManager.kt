package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.Utils.hexEncode
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.docker.model.ContainerCreateRequest
import de.fraunhofer.aisec.glue.docker.model.HostConfig
import de.fraunhofer.aisec.glue.docker.model.PortBinding
import fhg.glue.odrl.GlueContainerPipeline
import fhg.glue.odrl.GlueOdrlIdentifiers
import fhg.glue.odrl.OdrlAction
import fhg.glue.odrl.OdrlIdentifiers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import kotlin.random.Random

// TODO: This is fine for now, but for later we really want to make this part more generic to support other runtimes
//  (k8s) in the future
class ContainerManager(private val objectMapper: ObjectMapper, dockerClient: DockerClient? = null) : Closeable {
    private val pipelines = mutableMapOf<String, ContainerPipeline>()
    private val pipelineMutex = Mutex()
    private val dockerClient = dockerClient ?: DockerClient(System.getenv("DOCKER_SOCKET") ?: "/var/run/docker.sock")

    private fun dockerReqsForPipeline(pipelineDef: GlueContainerPipeline): MutableList<ContainerCreateRequest> {
        return pipelineDef.containers.map { c ->
            // TODO: Provide configurable defaults for other parameters
            // TODO: Insert c.params as a config map of some kind (probably a ro module with a single file)
            ContainerCreateRequest(
                image = c.image,
                hostConfig =
                    HostConfig(
                        // TODO: In case allowNetwork is true, use an isolated bridge instead of the default bridge
                        networkMode = if (c.allowNetwork) "bridge" else "none",
                    ),
            )
        }.toMutableList()
    }

    private fun createPipelineName(): String =
        generateSequence { Random.Default.nextBytes(8).hexEncode() }.first { n -> !pipelines.contains(n) }

    private suspend fun injectConfigs(pipeline: ContainerPipeline) {
        for (i in pipeline.containers.indices) {
            val nextAddress = pipeline.containers.getOrNull(i + 1)?.name

            val config =
                ContainerConfig(
                    // TODO: If the connector is running inside a container, get its name instead or set up an alias.
                    //       If it is not, get the gateway (host) address for any of the containers networks
                    controller = "TODO",
                    nextContainer = nextAddress,
                )
            pipeline.containers[i].copyToContainer {
                writeFile("config.json", objectMapper.writeValueAsBytes(config))
            }
        }
    }

    suspend fun createPipelineFromAction(
        action: OdrlAction,
        publishPorts: List<String> = emptyList(),
    ): Pair<String, ContainerPipeline> {
        assert(action.uid == GlueOdrlIdentifiers.DEPLOY_PIPELINE) { "Only deploy pipeline actions are allowed" }

        val pipelineDefs =
            action.refinements.filter { r ->
                r.leftOperand.uid == GlueOdrlIdentifiers.HAS_CONTAINER_PIPELINE && r.operator.operator == OdrlIdentifiers.EQ
            }

        if (pipelineDefs.isEmpty()) {
            throw Exception("A pipeline deploy action does not contain a pipeline definition")
        }
        // TODO: Is there a case where this could be fine (i.e. if further constraints exist)?
        if (pipelineDefs.size > 1) {
            throw Exception("A pipeline deploy action contains multiple pipeline definitions")
        }

        val pipelineDef =
            pipelineDefs[0].rightOperand.value as? GlueContainerPipeline ?: throw Exception(
                "Unexpected right operand type for container pipeline constraint",
            )
        val createRequests = dockerReqsForPipeline(pipelineDef)

        if (createRequests.isEmpty()) {
            throw Exception("The requested container pipeline is empty")
        }

        // This exposes the provided ports dynamically
        // To find the port, the container can be inspected after it is running
        createRequests[0] =
            createRequests[0].copy(
                exposedPorts = publishPorts.associateWith { emptyMap<Any, Any>() },
                hostConfig =
                    createRequests[0].hostConfig!!.copy(
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

        pipelineMutex.withLock {
            val pipelineName = createPipelineName()
            val pipeline =
                ContainerPipeline.create(
                    createRequests.mapIndexed { i, r -> "pipeline-$pipelineName-$i" to r },
                    dockerClient,
                )

            injectConfigs(pipeline)

            pipeline.start()
            pipelines[pipelineName] = pipeline

            return pipelineName to pipeline
        }
    }

    suspend fun deletePipeline(pipelineName: String): Boolean =
        pipelineMutex.withLock {
            return pipelines.remove(pipelineName)?.also { pipeline ->
                // TODO: Allow for a timeout on stop before removing and consider errors
                pipeline.stop()
                pipeline.remove()
            } != null
        }

    override fun close() {
        dockerClient.close()
    }

    suspend fun getPipelineStatus(id: String): Boolean =
        pipelineMutex.withLock {
            val pipeline = pipelines[id] ?: return false

            // TODO: Check the integrity of the running containers (using healthchecks if available)
            return true
        }
}
