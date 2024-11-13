package de.fraunhofer.aisec.glue.docker

import de.fraunhofer.aisec.glue.docker.model.ContainerInspectResponse
import de.fraunhofer.aisec.glue.docker.tar.TarWriter
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*

class Container internal constructor(val client: DockerClient, val name: String) {
    suspend fun start() {
        client.httpClient.post("${DockerClient.BASE_URL}/containers/$name/start")
    }

    suspend fun stop() {
        client.httpClient.post("${DockerClient.BASE_URL}/containers/$name/stop")
    }

    suspend fun kill() {
        client.httpClient.post("${DockerClient.BASE_URL}/containers/$name/kill")
    }

    suspend fun inspect(): ContainerInspectResponse {
        val resp = client.httpClient.get("${DockerClient.BASE_URL}/containers/$name/json")
        return resp.body<ContainerInspectResponse>()
    }

    suspend fun remove(
        deleteVolumes: Boolean = false,
        force: Boolean = false,
    ) {
        client.httpClient.delete("${DockerClient.BASE_URL}/containers/$name") {
            parameter("v", deleteVolumes)
            parameter("force", force)
        }
    }

    suspend fun copyToContainer(
        basePath: String = "/",
        block: TarWriter.() -> Unit,
    ) {
        client.httpClient.put("${DockerClient.BASE_URL}/containers/$name/archive") {
            parameter("path", basePath)
            setBody(
                OutputStreamContent({
                    TarWriter(this).use { t -> t.block() }
                }, ContentType("application", "x-tar")),
            )
        }
    }
}
