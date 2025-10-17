package de.fraunhofer.aisec.glue.pipeline

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.OdrlPolicy
import java.io.Closeable

abstract class ContainerPipeline(val name: String, val contractId: String, val targetId: String) : Closeable {
    // TODO: This should be persisted somewhere in case we crash
    val placeholderValues = mutableMapOf<String, JsonNode>()

    abstract suspend fun start()

    abstract suspend fun stop()

    abstract suspend fun remove()

    // TODO: Split this up into info and status and cache info

    /**
     * Get up-to-date info on the container at the specified index.
     */
    abstract suspend fun getContainerInfo(index: Int): ContainerInfo?

    /**
     * Get up-to-date info on all containers.
     */
    abstract suspend fun getContainerInfo(): List<ContainerInfo>

    /**
     * Inject a configuration file into the container.
     */
    abstract suspend fun injectConfig(
        index: Int,
        config: ContainerConfig,
    )

    /**
     * Associate a new contract with this pipeline as a follow-up contract.
     */
    abstract suspend fun addContract(
        peer: String,
        contract: OdrlPolicy,
    ): Boolean

    /**
     * Get all follow-up contracts from this pipeline.
     */
    abstract suspend fun contracts(peer: String): Set<OdrlPolicy>

    abstract suspend fun addRemotePipeline(remotePipeline: RemotePipeline): Boolean

    abstract suspend fun remotePipelines(): Set<RemotePipeline>
}
