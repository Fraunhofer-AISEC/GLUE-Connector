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
package de.fraunhofer.aisec.glue.docker

import de.fraunhofer.aisec.glue.Utils.hexEncode
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test

class DockerClientTest {
    private val client = DockerClient.fromEnv()

    @Test
    fun testPing() {
        runBlocking {
            client.ping()
        }
    }

    @Test
    fun testContainerFlow() {
        runBlocking {
            val name = Random.Default.nextBytes(10).hexEncode()
            val httpEchoImage = System.getenv("HTTP_ECHO_IMAGE_NAME") ?: "container-pipeline/http-echo"
            val container = client.containers.create(name, httpEchoImage)
            try {
                container.start()
                container.stop()
            } finally {
                container.remove()
            }
        }
    }

    @Test
    fun testNetworkFlow() {
        runBlocking {
            val name = Random.Default.nextBytes(10).hexEncode()
            val net = client.networks.create(name, "10.0.${Random.Default.nextInt(256)}.0/24")
            net.remove()
        }
    }
}
