package de.fraunhofer.aisec.uc_container_orchestration

import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        println("For this to work, we first need to build the test container")
        println(" \$ docker build -t test-container:latest test-container")

        val testContainers = sequenceOf("test-container-1", "test-container-2", "test-container-3")

        val pipeline = ContainerPipeline.create(testContainers.map { name -> Pair(name, "test-container:latest") })

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