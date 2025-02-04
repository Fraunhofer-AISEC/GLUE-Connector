package de.fraunhofer.aisec.glue.pipeline

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.Utils.hexEncode
import de.fraunhofer.aisec.glue.pipeline.docker.DockerBackend
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class PipelineManager(
    private val objectMapper: ObjectMapper,
    private val backend: Backend = DockerBackend(objectMapper),
) {
    private val pipelines = mutableMapOf<String, ContainerPipeline>()
    private val pipelineMutex = Mutex()

    private fun createPipelineName(): String =
        generateSequence { Random.Default.nextBytes(8).hexEncode() }.first { n -> !pipelines.contains(n) }

    suspend fun createPipeline(pipelineSpec: GlueContainerPipeline, contractId: String, targetId: String): ContainerPipeline =
        pipelineMutex.withLock {
            val name = createPipelineName()
            val pipeline = backend.createPipeline(name, pipelineSpec, contractId, targetId)

            try {
                val containerInfo = pipeline.getContainerInfo()
                for (i in containerInfo.indices) {
                    val current = containerInfo[i]
                    val next = containerInfo.getOrNull(i + 1)?.name

                    pipeline.injectConfig(
                        i,
                        ContainerConfig(
                            controller = backend.getConnectorIp(current),
                            nextContainer = next,
                            parameters = pipelineSpec.container[i].parameter,
                        ),
                    )
                }

                pipeline.start()

                pipelines[name] = pipeline
                pipeline
            } catch (t: Throwable) {
                pipeline.remove()
                throw t
            }
        }

    suspend fun deletePipeline(pipelineName: String): Boolean =
        pipelineMutex.withLock {
            return pipelines.remove(pipelineName)?.also { pipeline ->
                // TODO: Allow for a timeout on stop before removing and consider errors
                pipeline.stop()
                pipeline.remove()
                // Note: We are not calling close here since that just invokes stop and remove in the background.
            } != null
        }

    suspend fun getPipelineStatus(id: String): Boolean =
        pipelineMutex.withLock {
            val pipeline = pipelines[id] ?: return false
            // TODO: Check the integrity of the running containers (using healthchecks if available)
            return true
        }

    suspend fun getPipelineById(id: String): ContainerPipeline? = pipelineMutex.withLock { pipelines[id] }

    suspend fun getPipelineByIp(ip: String): ContainerPipeline? = pipelineMutex.withLock {
        backend.getPipelineFromIp(ip)?.let { id -> getPipelineById(id) }
    }
}
