package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The behavior to apply when the container exits. The default is not to restart.  An ever increasing delay (double the previous delay, starting at 100ms) is added before each restart to prevent flooding the server.
 *
 * @param name - Empty string means not to restart - `no` Do not automatically restart - `always` Always restart - `unless-stopped` Restart always except when the user has manually stopped the container - `on-failure` Restart only when the container exit code is non-zero
 * @param maximumRetryCount If `on-failure` is used, the number of times to retry before giving up.
 */
data class RestartPolicy(
    // - Empty string means not to restart - `no` Do not automatically restart - `always` Always restart - `unless-stopped` Restart always except when the user has manually stopped the container - `on-failure` Restart only when the container exit code is non-zero
    @param:JsonProperty("Name")
    val name: String? = null,
    // If `on-failure` is used, the number of times to retry before giving up.
    @param:JsonProperty("MaximumRetryCount")
    val maximumRetryCount: Int? = null,
)
