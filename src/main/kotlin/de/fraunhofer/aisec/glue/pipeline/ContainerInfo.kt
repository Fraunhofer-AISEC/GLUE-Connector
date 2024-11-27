package de.fraunhofer.aisec.glue.pipeline

data class ContainerInfo(
    /**
     * The name or ID of the container
     */
    val name: String,
    /**
     * All IP addresses the container is reachable on.
     * It is assumed that all of them are reachable from the connector.
     */
    val ipAddresses: List<String>,
    /**
     * All ports exposed by the container.
     */
    val ports: List<ContainerPort>,
    /**
     * The current status if the container
     */
    val status: ContainerStatus,
    /**
     * The process id of the container's entrypoint.
     * If the container is not running, this field is null.
     */
    val pid: Int? = null,
    /**
     * The exit code for the container's entrypoint.
     * This is set to a non-null value after the container exited.
     */
    val exitCode: Int? = null,
    /**
     * This map contains backend-specific properties.
     * Everything in here is considered unstable and should only be used by the respective backend.
     */
    val backendSpecific: Map<String, Any> = emptyMap(),
) {
    enum class ContainerStatus {
        /**
         * The container has not started yet or has exited.
         */
        DOWN,

        /**
         * The container has started but health checks are not yet successful.
         */
        STARTED,

        /**
         * The container has started and responds to health probes.
         */
        HEALTHY,
    }
}
