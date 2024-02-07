package de.fraunhofer.aisec.uc_container_orchestration.docker_client

import io.ktor.client.request.*

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

    suspend fun remove(deleteVolumes: Boolean = false, force: Boolean = false) {
        client.httpClient.delete("${DockerClient.BASE_URL}/containers/$name") {
            parameter("v", deleteVolumes)
            parameter("force", force)
        }
    }

}