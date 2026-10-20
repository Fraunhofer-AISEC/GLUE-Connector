/*
 * Copyright 2024-2025 Fraunhofer AISEC
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

/**
 * This interface abstracts over all possible backends that a container pipeline may have.
 * Currently, only the docker backend exists, but other runtimes (i.e. k8s) may come later.
 */
interface Backend {
    /**
     * Create a new pipeline from the provided ODRL object
     */
    suspend fun createPipeline(
        id: String,
        pipelineSpec: List<AugmentedGlueContainer>,
        contractId: String,
        targetId: String,
    ): ContainerPipeline

    /**
     * Get the IP address of the coordinator relative to a container.
     */
    suspend fun getConnectorIp(relativeTo: ContainerInfo): String

    /**
     * Get the pipeline ID from an IP address belonging to a container.
     * This operation return null in cases where the IP does not belong to any pipeline
     */
    suspend fun getPipelineFromIp(ipAddress: String): String?
}
