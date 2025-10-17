package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * An open port on a container
 *
 * @param privatePort Port on the container
 * @param type
 * @param ip Host IP address that the container's port is mapped to
 * @param publicPort Port exposed on the host
 */
data class Port(
    // Port on the container
    @param:JsonProperty("PrivatePort")
    val privatePort: Int,
    @param:JsonProperty("Type")
    val type: Type,
    // Host IP address that the container's port is mapped to
    @param:JsonProperty("IP")
    val ip: String? = null,
    // Port exposed on the host
    @param:JsonProperty("PublicPort")
    val publicPort: Int? = null,
) {
    enum class Type(val value: String) {
        @JsonProperty("tcp")
        TCP("tcp"),

        @JsonProperty("udp")
        UDP("udp"),

        @JsonProperty("sctp")
        SCTP("sctp"),
    }
}
