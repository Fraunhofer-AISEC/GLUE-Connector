package de.fraunhofer.aisec.glue.docker

import de.fraunhofer.aisec.glue.docker.model.ContainerInspectResponse
import de.fraunhofer.aisec.glue.docker.tar.TarWriter
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*

class Container internal constructor(val client: DockerClient, val name: String) {
    suspend fun start() {
        client.httpClient.post(client.endpoint("containers/$name/start"))
    }

    suspend fun stop() {
        client.httpClient.post(client.endpoint("containers/$name/stop"))
    }

    suspend fun kill() {
        client.httpClient.post(client.endpoint("containers/$name/kill"))
    }

    suspend fun inspect(): ContainerInspectResponse {
        val resp = client.httpClient.get(client.endpoint("containers/$name/json"))
        return resp.body<ContainerInspectResponse>()
    }

    suspend fun remove(
        deleteVolumes: Boolean = false,
        force: Boolean = false,
    ) {
        client.httpClient.delete(client.endpoint("containers/$name")) {
            parameter("v", deleteVolumes)
            parameter("force", force)
        }
    }

    suspend fun copyToContainer(
        basePath: String = "/",
        block: TarWriter.() -> Unit,
    ) {
        client.httpClient.put(client.endpoint("containers/$name/archive")) {
            parameter("path", basePath)
            setBody(
                OutputStreamContent({
                    TarWriter(this).use { t -> t.block() }
                }, ContentType("application", "x-tar")),
            )
        }
    }
}
