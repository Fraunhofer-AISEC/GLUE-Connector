package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.Utils.hexEncode
import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.docker.model.ContainerCreateRequest
import de.fraunhofer.aisec.glue.docker.model.HostConfig
import de.fraunhofer.aisec.glue.docker.model.PortBinding
import de.fraunhofer.iosb.odrl.OdrlAction
import de.fraunhofer.iosb.odrl.OdrlConstraint
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import de.fraunhofer.iosb.odrl.OdrlLogicalConstraint
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import kotlin.random.Random

//  (k8s) in the future
class ContainerManager(private val objectMapper: ObjectMapper, dockerClient: DockerClient? = null) : Closeable {
    private val pipelines = mutableMapOf<String, ContainerPipeline>()
    private val pipelineMutex = Mutex()
    private val dockerClient = dockerClient ?: DockerClient(System.getenv("DOCKER_SOCKET") ?: "/var/run/docker.sock")

    private fun dockerReqsForPipeline(pipelineDef: GlueContainerPipeline): MutableList<ContainerCreateRequest> {
        return pipelineDef.container.map { c ->
            ContainerCreateRequest(
                image = c.image,
                hostConfig =
                    HostConfig(
                        networkMode = if (c.allowNetwork == true) "bridge" else "none",
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
        assert(action.uid.toString() == GlueIdentifiers.DEPLOY_PIPELINE) { "Only deploy pipeline actions are allowed" }

        val pipelineDefs =
            action.refinement.filter { r ->
                when (r) {
                    is OdrlConstraint -> (r.leftOperand.uid.toString() == GlueIdentifiers.HAS_CONTAINER_PIPELINE && r.operator.uid.toString() == OdrlIdentifiers.EQ)
                    is OdrlLogicalConstraint -> TODO("Implement pipeline extraction from logical constraints?")
                    else -> throw IllegalStateException("Constraints of type ${r.javaClass.name} not supported")
                }
            }.map { r -> r as OdrlConstraint }

        if (pipelineDefs.isEmpty()) {
            throw Exception("A pipeline deploy action does not contain a pipeline definition")
        }
        if (pipelineDefs.size > 1) {
            throw Exception("A pipeline deploy action contains multiple pipeline definitions")
        }

        val pipelineDef =
            pipelineDefs[0].rightOperand[0] as? GlueContainerPipeline ?: throw Exception(
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

            return true
        }
}
