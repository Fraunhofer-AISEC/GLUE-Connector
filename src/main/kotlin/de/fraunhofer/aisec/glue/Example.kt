package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.docker.DockerClient
import de.fraunhofer.aisec.glue.docker.model.ContainerCreateRequest
import de.fraunhofer.aisec.glue.docker.model.HostConfig
import de.fraunhofer.aisec.glue.docker.model.PortBinding
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlin.system.exitProcess

/*
fun main() {
    runBlocking {
        DockerClient().use { client ->
            println("For this to work, we first need to build the test container")
            println(" \$ docker build -t test-container:latest test-container")

            val testContainers = sequenceOf("test-container-1", "test-container-2", "test-container-3")

            val pipeline = ContainerPipeline.create(
                testContainers.map { name -> Pair(name, "test-container:latest") },
                client
            )

            pipeline.start()
            println("The pipeline should be started now.")
            println("The containers are:")
            for (container in testContainers) {
                println("  $container")
            }
            println("Each container can only reach the one before and after, as well as the host")
            println("If this application is running inside a container as well, it is also reachable")
            println()
            println("Press ENTER to continue...")
            readln()
            println("Shutting down...")
            pipeline.stop()
            pipeline.remove()
        }
    }
}
*/

fun createContainerCreateReq(
    image: String,
    entrypoint: List<String>? = null,
    cmd: List<String>? = null,
    ports: Map<Int, Int>? = null,
    env: Map<String, String>? = null,
): ContainerCreateRequest {
    return ContainerCreateRequest(
        image = image,
        env = env?.map { (k, v) -> "$k=$v" },
        entrypoint = entrypoint,
        cmd = cmd,
        exposedPorts = ports?.keys?.associate { p -> Pair("$p/tcp", JsonObject(mapOf())) },
        hostConfig =
            HostConfig(
                networkMode = "none",
                portBindings =
                    ports?.toList()?.associate { (k, v) ->
                        Pair(
                            "$k/tcp",
                            listOf(
                                PortBinding(hostIp = "::", hostPort = "$v"),
                                PortBinding(hostIp = "0.0.0.0", hostPort = "$v"),
                            ),
                        )
                    },
            ),
    )
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Expected the docker image prefix as argument")
        exitProcess(1)
    }
    val prefix = args[0]

    runBlocking {
        DockerClient().use { client ->
            val containers =
                listOf(
                    Pair("proxy", createContainerCreateReq(image = "$prefix/proxy", cmd = listOf("8080", "http-echo:8080"))),
                    Pair("http-echo", createContainerCreateReq(image = "$prefix/http-echo", cmd = listOf("0.0.0.0:8080"))),
                )

            val pipeline = ContainerPipeline.create(containers, client)
            pipeline.start()

            readln()

            pipeline.stop()
            pipeline.remove()
        }
    }
}
