package de.fraunhofer.aisec.glue.pipeline

import de.fraunhofer.aisec.glue.Utils.hexEncode
import de.fraunhofer.aisec.glue.config.ConfigManager
import de.fraunhofer.aisec.glue.pipeline.atls.AtlsConfig
import de.fraunhofer.aisec.glue.pipeline.atls.AtlsParameter
import de.fraunhofer.iosb.odrl.glue.GlueAtlsProxyConfiguration
import de.fraunhofer.iosb.odrl.glue.GlueContainer
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.sockets.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.InetSocketAddress
import kotlin.random.Random

class PipelineManager(
    private val remoteManager: RemotePipelineManager,
    private val backend: Backend,
    config: ConfigManager,
) {
    private val pipelines = mutableMapOf<String, ContainerPipeline>()
    private val pipelineMutex = Mutex()
    private val pipelineConfig = config.getConfig(::AtlsConfig)

    private fun createPipelineName(): String = generateSequence { Random.nextBytes(8).hexEncode() }.first { n -> !pipelines.contains(n) }

    private fun createAtlsContainer(proxyConfig: GlueAtlsProxyConfiguration): AugmentedGlueContainer {
        return AugmentedGlueContainer(
            GlueContainer(
                pipelineConfig.proxyContainer,
                pipelineConfig.proxyHash,
                true,
                listOf(
                    AtlsParameter(
                        mode = proxyConfig.proxyMode,
                        cmcAddress =
                            pipelineConfig.cmcAddress.let {
                                when (val addr = it.toJavaAddress()) {
                                    is InetSocketAddress -> "${addr.hostString}:${addr.port}"
                                    else -> addr.toString()
                                }
                            },
                        caCertificate = File(pipelineConfig.caCertPath).readText(),
                        proxyEndpoint = proxyConfig.proxyEndpoint
                    ),
                ),
            ),
            buildMap {
                // If the CMC is made available via a unix socket, we need to mount it into the container
                // TODO: This way of handling this adds the constraint that the CMC must be on the same path on all
                //  possible nodes, which is reasonable but should be documented.
                if (pipelineConfig.cmcAddress is UnixSocketAddress) {
                    val path = (pipelineConfig.cmcAddress as UnixSocketAddress).path
                    put(path, path)
                }
            },
        )
    }

    /**
     * Augment the container pipeline object by properly configuring all ATLS proxy images
     */
    private fun augmentPipelineSpec(originalSpec: GlueContainerPipeline): List<AugmentedGlueContainer> {
        val augmentedSpec = mutableListOf<AugmentedGlueContainer>()
        originalSpec.atlsProxyIn?.let {
            LOG.atDebug {
                message = "Adding ATLS input proxy container"
                payload = mapOf("atlsProxyIn" to it)
            }
            augmentedSpec += createAtlsContainer(it)
        }
        originalSpec.container.forEach { augmentedSpec += AugmentedGlueContainer(it, emptyMap()) }
        originalSpec.atlsProxyOut?.let {
            LOG.atDebug {
                message = "Adding ATLS output proxy container"
                payload = mapOf("atlsProxyOut" to it)
            }
            augmentedSpec += createAtlsContainer(it)
        }
        return augmentedSpec
    }

    suspend fun createPipeline(
        pipelineSpec: GlueContainerPipeline,
        contractId: String,
        targetId: String,
    ): ContainerPipeline =
        pipelineMutex.withLock {
            val augmentedSpec = augmentPipelineSpec(pipelineSpec)

            val name = createPipelineName()
            val pipeline = backend.createPipeline(name, augmentedSpec, contractId, targetId)

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
                            parameters = augmentedSpec[i].base.parameter,
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
                pipeline.remotePipelines().forEach { remoteManager.delete(it.id) }
                // Note: We are not calling close here since that just invokes stop and remove in the background.
            } != null
        }

    suspend fun getPipelines(): Map<String, ContainerPipeline> = pipelineMutex.withLock { pipelines }

    suspend fun getPipelineStatus(id: String): Boolean =
        pipelineMutex.withLock {
            val pipeline = pipelines[id] ?: return false
            // TODO: Check the integrity of the running containers (using healthchecks if available)
            return true
        }

    suspend fun getPipelineById(id: String): ContainerPipeline? = pipelineMutex.withLock { pipelines[id] }

    suspend fun getPipelineByIp(ip: String): ContainerPipeline? =
        pipelineMutex.withLock {
            backend.getPipelineFromIp(ip)?.let(pipelines::get)
        }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
