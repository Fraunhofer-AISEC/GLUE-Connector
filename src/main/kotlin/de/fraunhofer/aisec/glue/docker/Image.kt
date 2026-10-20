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
package de.fraunhofer.aisec.glue.docker

import de.fraunhofer.aisec.glue.docker.model.ImageInspect
import io.ktor.client.call.*
import io.ktor.client.request.*

class Image internal constructor(
    val client: DockerClient,
    val name: String,
) {
    suspend fun inspect(): ImageInspect {
        val response = client.httpClient.get(client.endpoint("images/$name/json"))
        return response.body<ImageInspect>()
    }
}
