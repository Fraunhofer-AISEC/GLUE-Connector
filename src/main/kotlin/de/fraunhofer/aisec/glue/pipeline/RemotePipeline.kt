/*
 * Copyright 2025 Fraunhofer AISEC
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

import de.fraunhofer.aisec.glue.Utils
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess

class RemotePipeline(
    // Local ID of the pipeline.
    val id: String,
    // ID of this pipeline on the remote connector.
    val remoteId: String,
    // Peer URL to reach the owning connector on.
    val peer: Url,
    // Ports exposed by the pipeline.
    val ports: List<ContainerPort>,
    // Details regarding the governing contract of this pipeline
    val contractId: String,
    val targetId: String,
    val actionId: String,
    private val httpClient: HttpClient,
) {
    /**
     * Return the status of this pipeline.
     * Currently, it is only reported if the pipeline is up.
     */
    suspend fun status(): Boolean {
        val statusResp = httpClient.get(URLBuilder(peer).appendPathSegments("pipeline", remoteId).build())
        // Note: This eats a bunch of errors like unauthorized, bad gateway, ...
        return statusResp.status.isSuccess()
    }

    /**
     * Stop the remote pipeline.
     * Returns `true` if the remote pipeline is successfully stopped.
     */
    internal suspend fun stop() {
        val deleteResp = httpClient.delete(URLBuilder(peer).appendPathSegments("pipeline", remoteId).build())
        if (!deleteResp.status.isSuccess()) {
            throw Utils.makeHttpException(deleteResp)
        }
    }
}
