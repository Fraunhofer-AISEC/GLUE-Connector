/*
 * Copyright 2024-2026 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.aisec.glue.pipeline

import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iosb.odrl.OdrlPolicy
import java.io.Closeable

abstract class ContainerPipeline(
    val name: String,
    val contractId: String,
    val targetId: String,
) : Closeable {
    val placeholderValues = mutableMapOf<String, JsonNode>()

    abstract suspend fun start()

    abstract suspend fun stop()

    abstract suspend fun remove()

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

    abstract suspend fun contracts(): Map<String, Set<OdrlPolicy>>

    /**
     * Get all follow-up contracts from this pipeline.
     */
    abstract suspend fun contracts(peer: String): Set<OdrlPolicy>

    abstract suspend fun addRemotePipeline(remotePipeline: RemotePipeline): Boolean

    abstract suspend fun remotePipelines(): Set<RemotePipeline>
}
