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

    private val httpClient: HttpClient
) {
    /**
     * Return the status of this pipeline.
     * Currently, it is only reported if the pipeline is up.
     * TODO: Expand the status report to be actually useful for users.
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
