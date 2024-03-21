package de.fraunhofer.aisec.glue.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A request for devices to be sent to device drivers
 *
 * @param driver
 * @param count
 * @param deviceIDs
 * @param capabilities A list of capabilities; an OR list of AND lists of capabilities.
 * @param options Driver-specific options, specified as a key/value pairs. These options are passed directly to the driver.
 */
@Serializable
data class DeviceRequest(
    @SerialName(value = "Driver")
    val driver: String? = null,
    @SerialName(value = "Count")
    val count: Int? = null,
    @SerialName(value = "DeviceIDs")
    val deviceIDs: List<String>? = null,
    // A list of capabilities; an OR list of AND lists of capabilities.
    @SerialName(value = "Capabilities")
    val capabilities: List<List<String>>? = null,
    // Driver-specific options, specified as a key/value pairs. These options are passed directly to the driver.
    @SerialName(value = "Options")
    val options: Map<String, String>? = null,
)
