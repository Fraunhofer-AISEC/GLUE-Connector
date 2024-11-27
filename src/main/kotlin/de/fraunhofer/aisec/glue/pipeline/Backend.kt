package de.fraunhofer.aisec.glue.pipeline

import fhg.glue.odrl.GlueContainerPipeline

/**
 * This interface abstracts over all possible backends that a container pipeline may have.
 * Currently, only the docker backend exists, but other runtimes (i.e. k8s) may come later.
 */
interface Backend {
    /**
     * Create a new pipeline from the provided ODRL object
     */
    suspend fun createPipeline(
        id: String,
        policyObject: GlueContainerPipeline,
    ): ContainerPipeline

    //       If we want to expose a HTTP endpoint to the connector, this may be required to ensure we listen on the
    //       correct endpoints.
    //       If we could for example create a network where all containers are but where they could only talk to us,
    //       then we could just use our IP in that network.
    //
    //       For the moment I see two options split over two scenarios:
    //       - Use a separate bridge network that all containers are in that is isolated and has icc disabled.
    //         If we are running directly on the host, this would mean that we would simply need to bind a port on the
    //         hosts IP in that subnet and are done.
    //         If we are running in a container however things become more complicated. Since an isolated network is
    //         required, the default docker port forwarding does not seem to work
    //         (https://github.com/moby/moby/issues/36174).
    //         We could perhaps create a simple forwarding rule for our container that binds it to the internal networks
    //         ip on the host without being part of it. This however would have to be set up beforehand.
    //         (This is also a problem for exposing our pipeline entry points to the network (we could use haproxy there
    //         apparently))
    //
    //       - Listen on each network separately. This is more work but would not require any previous setup if we are
    //         in a container.

    /**
     * Get the IP address of the coordinator relative to a container.
     */
    suspend fun getConnectorIp(relativeTo: ContainerInfo): String
}
