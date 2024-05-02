package de.fraunhofer.aisec.glue

/**
 * The Ingress class is used to map HTTP calls to the external interface of the UC engine to direct calls to the
 * Note that this class does not perform any policy checks.
 */
class Ingress(private val contract: String, private val pipeline: ContainerPipeline) {
    suspend fun resolveContainerURL(path: String): String? {
        //       Eventually, we may want the caller to be able to specify its own port and the destination container by
        //       name.
        //       We may also need to allow the path to change.
        val containerInfo = pipeline.containers.getOrNull(0)?.inspect() ?: return null
        // We can use any IP address of the container, since we are in all networks.
        //       This is currently not the case if the pipeline contains only one container
        val containerNetwork = containerInfo.networkSettings?.networks?.values?.firstOrNull() ?: return null
        val containerIp = containerNetwork.ipAddress ?: return null

        return "http://$containerIp:8080/$path"
    }
}
