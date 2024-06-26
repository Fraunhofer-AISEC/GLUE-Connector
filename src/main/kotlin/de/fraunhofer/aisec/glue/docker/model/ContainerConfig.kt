package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Configuration for a container that is portable between hosts.  When used as `ContainerConfig` field in an image, `ContainerConfig` is an optional field containing the configuration of the container that was last committed when creating the image.  Previous versions of Docker builder used this field to store build cache, and it is not in active use anymore.
 *
 * @param hostname The hostname to use for the container, as a valid RFC 1123 hostname.
 * @param domainname The domain name to use for the container.
 * @param user The user that commands are run as inside the container.
 * @param attachStdin Whether to attach to `stdin`.
 * @param attachStdout Whether to attach to `stdout`.
 * @param attachStderr Whether to attach to `stderr`.
 * @param exposedPorts An object mapping ports to an empty object in the form:  `{\"<port>/<tcp|udp|sctp>\": {}}`
 * @param tty Attach standard streams to a TTY, including `stdin` if it is not closed.
 * @param openStdin Open `stdin`
 * @param stdinOnce Close `stdin` after one attached client disconnects
 * @param env A list of environment variables to set inside the container in the form `[\"VAR=value\", ...]`. A variable without `=` is removed from the environment, rather than to have an empty value.
 * @param cmd Command to run specified as a string or an array of strings.
 * @param healthcheck
 * @param argsEscaped Command is already escaped (Windows only)
 * @param image The name (or reference) of the image to use when creating the container, or which was used when the container was created.
 * @param volumes An object mapping mount point paths inside the container to empty objects.
 * @param workingDir The working directory for commands to run in.
 * @param entrypoint The entry point for the container as a string or an array of strings.  If the array consists of exactly one empty string (`[\"\"]`) then the entry point is reset to system default (i.e., the entry point used by docker when there is no `ENTRYPOINT` instruction in the `Dockerfile`).
 * @param networkDisabled Disable networking for the container.
 * @param macAddress MAC address of the container.
 * @param onBuild `ONBUILD` metadata that were defined in the image's `Dockerfile`.
 * @param labels User-defined key/value metadata.
 * @param stopSignal Signal to stop a container as a string or unsigned integer.
 * @param stopTimeout Timeout to stop a container in seconds.
 * @param shell Shell for when `RUN`, `CMD`, and `ENTRYPOINT` uses a shell.
 */
data class ContainerConfig(
    // The hostname to use for the container, as a valid RFC 1123 hostname.
    @JsonProperty("Hostname")
    val hostname: String? = null,
    // The domain name to use for the container.
    @JsonProperty("Domainname")
    val domainname: String? = null,
    // The user that commands are run as inside the container.
    @JsonProperty("User")
    val user: String? = null,
    // Whether to attach to `stdin`.
    @JsonProperty("AttachStdin")
    val attachStdin: Boolean? = false,
    // Whether to attach to `stdout`.
    @JsonProperty("AttachStdout")
    val attachStdout: Boolean? = true,
    // Whether to attach to `stderr`.
    @JsonProperty("AttachStderr")
    val attachStderr: Boolean? = true,
    // An object mapping ports to an empty object in the form:  `{\"<port>/<tcp|udp|sctp>\": {}}`
    @JsonProperty("ExposedPorts")
    val exposedPorts: Map<String, Any>? = null,
    // Attach standard streams to a TTY, including `stdin` if it is not closed.
    @JsonProperty("Tty")
    val tty: Boolean? = false,
    // Open `stdin`
    @JsonProperty("OpenStdin")
    val openStdin: Boolean? = false,
    // Close `stdin` after one attached client disconnects
    @JsonProperty("StdinOnce")
    val stdinOnce: Boolean? = false,
    // A list of environment variables to set inside the container in the form `[\"VAR=value\", ...]`. A variable without `=` is removed from the environment, rather than to have an empty value.
    @JsonProperty("Env")
    val env: List<String>? = null,
    // Command to run specified as a string or an array of strings.
    @JsonProperty("Cmd")
    val cmd: List<String>? = null,
    @JsonProperty("Healthcheck")
    val healthcheck: HealthConfig? = null,
    // Command is already escaped (Windows only)
    @JsonProperty("ArgsEscaped")
    val argsEscaped: Boolean? = false,
    // The name (or reference) of the image to use when creating the container, or which was used when the container was created.
    @JsonProperty("Image")
    val image: String? = null,
    // An object mapping mount point paths inside the container to empty objects.
    @JsonProperty("Volumes")
    val volumes: Map<String, Any>? = null,
    // The working directory for commands to run in.
    @JsonProperty("WorkingDir")
    val workingDir: String? = null,
    // The entry point for the container as a string or an array of strings.  If the array consists of exactly one empty string (`[\"\"]`) then the entry point is reset to system default (i.e., the entry point used by docker when there is no `ENTRYPOINT` instruction in the `Dockerfile`).
    @JsonProperty("Entrypoint")
    val entrypoint: List<String>? = null,
    // Disable networking for the container.
    @JsonProperty("NetworkDisabled")
    val networkDisabled: Boolean? = null,
    // MAC address of the container.
    @JsonProperty("MacAddress")
    val macAddress: String? = null,
    // `ONBUILD` metadata that were defined in the image's `Dockerfile`.
    @JsonProperty("OnBuild")
    val onBuild: List<String>? = null,
    // User-defined key/value metadata.
    @JsonProperty("Labels")
    val labels: Map<String, String>? = null,
    // Signal to stop a container as a string or unsigned integer.
    @JsonProperty("StopSignal")
    val stopSignal: String? = null,
    // Timeout to stop a container in seconds.
    @JsonProperty("StopTimeout")
    val stopTimeout: Int? = null,
    // Shell for when `RUN`, `CMD`, and `ENTRYPOINT` uses a shell.
    @JsonProperty("Shell")
    val shell: List<String>? = null,
)
