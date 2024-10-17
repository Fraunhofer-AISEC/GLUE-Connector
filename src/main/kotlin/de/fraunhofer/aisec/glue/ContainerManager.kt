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
import kotlin.random.Random

class ContainerManager {
    private val pipelines = mutableMapOf<String, ContainerPipeline>()
    private val pipelineMutex = Mutex()

    private fun dockerReqsForPipeline(pipelineDef: GlueContainerPipeline): MutableList<ContainerCreateRequest> {
        return pipelineDef.containers.map { c ->
            // TODO: Provide configurable defaults for other parameters
            // TODO: Insert c.params as a config map of some kind (probably a ro module with a single file)
            ContainerCreateRequest(
                // TODO: Allow for other hash algorithms?
                image = "${c.image}@sha256:${c.hash}",
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
                exposedPorts = publishPorts.associateWith { Any() },
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
            val pipeline = ContainerPipeline.create(
                createRequests.mapIndexed { i, r -> "pipeline-$pipelineName-$i" to r },
                DockerClient("/run/user/1000/docker-socket.sock"),
            )
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
}
