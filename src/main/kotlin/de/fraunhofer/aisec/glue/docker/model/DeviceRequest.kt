package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A request for devices to be sent to device drivers
 *
 * @param driver
 * @param count
 * @param deviceIDs
 * @param capabilities A list of capabilities; an OR list of AND lists of capabilities.
 * @param options Driver-specific options, specified as a key/value pairs. These options are passed directly to the driver.
 */
data class DeviceRequest(
    @param:JsonProperty("Driver")
    val driver: String? = null,
    @param:JsonProperty("Count")
    val count: Int? = null,
    @param:JsonProperty("DeviceIDs")
    val deviceIDs: List<String>? = null,
    // A list of capabilities; an OR list of AND lists of capabilities.
    @param:JsonProperty("Capabilities")
    val capabilities: List<List<String>>? = null,
    // Driver-specific options, specified as a key/value pairs. These options are passed directly to the driver.
    @param:JsonProperty("Options")
    val options: Map<String, String>? = null,
)
