package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * HealthcheckResult stores information about a single run of a healthcheck probe
 *
 * @param start Date and time at which this check started in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format with nano-seconds.
 * @param end Date and time at which this check ended in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format with nano-seconds.
 * @param exitCode ExitCode meanings:  - `0` healthy - `1` unhealthy - `2` reserved (considered unhealthy) - other values: error running probe
 * @param output Output from last check
 */
data class HealthcheckResult(
    // Date and time at which this check started in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format with nano-seconds.
    @JsonProperty("Start")
    val start: java.time.OffsetDateTime? = null,
    // Date and time at which this check ended in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format with nano-seconds.
    @JsonProperty("End")
    val end: String? = null,
    // ExitCode meanings:  - `0` healthy - `1` unhealthy - `2` reserved (considered unhealthy) - other values: error running probe
    @JsonProperty("ExitCode")
    val exitCode: Int? = null,
    // Output from last check
    @JsonProperty("Output")
    val output: String? = null,
)
