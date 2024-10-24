package de.fraunhofer.aisec.glue

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

class ContainerManager : Closeable {
    private val pipelines = mutableMapOf<String, ContainerPipeline>()
    private val pipelineMutex = Mutex()
    private val dockerClient = DockerClient(System.getenv("DOCKER_SOCKET") ?: "/var/run/docker.sock")

    private fun dockerReqsForPipeline(pipelineDef: GlueContainerPipeline): MutableList<ContainerCreateRequest> {
        return pipelineDef.containers.map { c ->
            ContainerCreateRequest(
                image = c.image,
                hostConfig =
                    HostConfig(
                        networkMode = if (c.allowNetwork) "bridge" else "none",
                    ),
            )
        }.toMutableList()
    }

    private fun createPipelineName(): String =
        generateSequence { Random.Default.nextBytes(8).hexEncode() }.first { n -> !pipelines.contains(n) }

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
                                        hostPort = "",
                                    ),
                                )
                            },
                    ),
            )

        pipelineMutex.withLock {
            val pipelineName = createPipelineName()
            val pipeline = ContainerPipeline.create(
                createRequests.mapIndexed { i, r -> "pipeline-$pipelineName-$i" to r },
                dockerClient,
            )
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

    suspend fun getPipelineStatus(id: String): Boolean = pipelineMutex.withLock {
        val pipeline = pipelines[id] ?: return false

        return true
    }
}
