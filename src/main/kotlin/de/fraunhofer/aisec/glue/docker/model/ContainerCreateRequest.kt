package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
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
 * @param hostConfig
 * @param networkingConfig
 */
data class ContainerCreateRequest(
    // The hostname to use for the container, as a valid RFC 1123 hostname.
    @param:JsonProperty("Hostname")
    val hostname: String? = null,
    // The domain name to use for the container.
    @param:JsonProperty("Domainname")
    val domainname: String? = null,
    // The user that commands are run as inside the container.
    @param:JsonProperty("User")
    val user: String? = null,
    // Whether to attach to `stdin`.
    @param:JsonProperty("AttachStdin")
    val attachStdin: Boolean? = false,
    // Whether to attach to `stdout`.
    @param:JsonProperty("AttachStdout")
    val attachStdout: Boolean? = true,
    // Whether to attach to `stderr`.
    @param:JsonProperty("AttachStderr")
    val attachStderr: Boolean? = true,
    // An object mapping ports to an empty object in the form:  `{\"<port>/<tcp|udp|sctp>\": {}}`
    @param:JsonProperty("ExposedPorts")
    val exposedPorts: Map<String, Any>? = null,
    // Attach standard streams to a TTY, including `stdin` if it is not closed.
    @param:JsonProperty("Tty")
    val tty: Boolean? = false,
    // Open `stdin`
    @param:JsonProperty("OpenStdin")
    val openStdin: Boolean? = false,
    // Close `stdin` after one attached client disconnects
    @param:JsonProperty("StdinOnce")
    val stdinOnce: Boolean? = false,
    // A list of environment variables to set inside the container in the form `[\"VAR=value\", ...]`. A variable without `=` is removed from the environment, rather than to have an empty value.
    @param:JsonProperty("Env")
    val env: List<String>? = null,
    // Command to run specified as a string or an array of strings.
    @param:JsonProperty("Cmd")
    val cmd: List<String>? = null,
    @param:JsonProperty("Healthcheck")
    val healthcheck: HealthConfig? = null,
    // Command is already escaped (Windows only)
    @param:JsonProperty("ArgsEscaped")
    val argsEscaped: Boolean? = false,
    // The name (or reference) of the image to use when creating the container, or which was used when the container was created.
    @param:JsonProperty("Image")
    val image: String? = null,
    // An object mapping mount point paths inside the container to empty objects.
    @param:JsonProperty("Volumes")
    val volumes: Map<String, Any>? = null,
    // The working directory for commands to run in.
    @param:JsonProperty("WorkingDir")
    val workingDir: String? = null,
    // The entry point for the container as a string or an array of strings.  If the array consists of exactly one empty string (`[\"\"]`) then the entry point is reset to system default (i.e., the entry point used by docker when there is no `ENTRYPOINT` instruction in the `Dockerfile`).
    @param:JsonProperty("Entrypoint")
    val entrypoint: List<String>? = null,
    // Disable networking for the container.
    @param:JsonProperty("NetworkDisabled")
    val networkDisabled: Boolean? = null,
    // MAC address of the container.
    @param:JsonProperty("MacAddress")
    val macAddress: String? = null,
    // `ONBUILD` metadata that were defined in the image's `Dockerfile`.
    @param:JsonProperty("OnBuild")
    val onBuild: List<String>? = null,
    // User-defined key/value metadata.
    @param:JsonProperty("Labels")
    val labels: Map<String, String>? = null,
    // Signal to stop a container as a string or unsigned integer.
    @param:JsonProperty("StopSignal")
    val stopSignal: String? = null,
    // Timeout to stop a container in seconds.
    @param:JsonProperty("StopTimeout")
    val stopTimeout: Int? = null,
    // Shell for when `RUN`, `CMD`, and `ENTRYPOINT` uses a shell.
    @param:JsonProperty("Shell")
    val shell: List<String>? = null,
    @param:JsonProperty("HostConfig")
    val hostConfig: HostConfig? = null,
    @param:JsonProperty("NetworkingConfig")
    val networkingConfig: NetworkingConfig? = null,
)
