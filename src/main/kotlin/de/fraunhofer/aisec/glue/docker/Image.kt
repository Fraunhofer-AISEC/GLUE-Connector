package de.fraunhofer.aisec.glue.docker

import de.fraunhofer.aisec.glue.docker.model.ImageInspect
import io.ktor.client.call.*
import io.ktor.client.request.*

class Image internal constructor(val client: DockerClient, val name: String) {
    suspend fun inspect(): ImageInspect {
        val response = client.httpClient.get("${DockerClient.UNIX_BASE_URL}/images/$name/json")
        return response.body<ImageInspect>()
    }
}
