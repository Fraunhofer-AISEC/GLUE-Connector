/*
 * Copyright 2025 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Configuration of the image. These fields are used as defaults when starting a container from the image.
 *
 * @param hostname The hostname to use for the container, as a valid RFC 1123 hostname.  <p><br /></p>  > **Note**: this field is always empty and must not be used.
 * @param domainname The domain name to use for the container.  <p><br /></p>  > **Note**: this field is always empty and must not be used.
 * @param user The user that commands are run as inside the container.
 * @param attachStdin Whether to attach to `stdin`.  <p><br /></p>  > **Note**: this field is always false and must not be used.
 * @param attachStdout Whether to attach to `stdout`.  <p><br /></p>  > **Note**: this field is always false and must not be used.
 * @param attachStderr Whether to attach to `stderr`.  <p><br /></p>  > **Note**: this field is always false and must not be used.
 * @param exposedPorts An object mapping ports to an empty object in the form:  `{\"<port>/<tcp|udp|sctp>\": {}}`
 * @param tty Attach standard streams to a TTY, including `stdin` if it is not closed.  <p><br /></p>  > **Note**: this field is always false and must not be used.
 * @param openStdin Open `stdin`  <p><br /></p>  > **Note**: this field is always false and must not be used.
 * @param stdinOnce Close `stdin` after one attached client disconnects.  <p><br /></p>  > **Note**: this field is always false and must not be used.
 * @param env A list of environment variables to set inside the container in the form `[\"VAR=value\", ...]`. A variable without `=` is removed from the environment, rather than to have an empty value.
 * @param cmd Command to run specified as a string or an array of strings.
 * @param healthcheck
 * @param argsEscaped Command is already escaped (Windows only)
 * @param image The name (or reference) of the image to use when creating the container, or which was used when the container was created.  <p><br /></p>  > **Note**: this field is always empty and must not be used.
 * @param volumes An object mapping mount point paths inside the container to empty objects.
 * @param workingDir The working directory for commands to run in.
 * @param entrypoint The entry point for the container as a string or an array of strings.  If the array consists of exactly one empty string (`[\"\"]`) then the entry point is reset to system default (i.e., the entry point used by docker when there is no `ENTRYPOINT` instruction in the `Dockerfile`).
 * @param networkDisabled Disable networking for the container.  <p><br /></p>  > **Note**: this field is always omitted and must not be used.
 * @param macAddress MAC address of the container.  <p><br /></p>  > **Note**: this field is always omitted and must not be used.
 * @param onBuild `ONBUILD` metadata that were defined in the image's `Dockerfile`.
 * @param labels User-defined key/value metadata.
 * @param stopSignal Signal to stop a container as a string or unsigned integer.
 * @param stopTimeout Timeout to stop a container in seconds.  <p><br /></p>  > **Note**: this field is always omitted and must not be used.
 * @param shell Shell for when `RUN`, `CMD`, and `ENTRYPOINT` uses a shell.
 */

data class ImageConfig(
    // The hostname to use for the container, as a valid RFC 1123 hostname.  <p><br /></p>  > **Note**: this field is always empty and must not be used.
    @get:JsonProperty("Hostname")
    val hostname: kotlin.String? = null,
    // The domain name to use for the container.  <p><br /></p>  > **Note**: this field is always empty and must not be used.
    @get:JsonProperty("Domainname")
    val domainname: kotlin.String? = null,
    // The user that commands are run as inside the container.
    @get:JsonProperty("User")
    val user: kotlin.String? = null,
    // Whether to attach to `stdin`.  <p><br /></p>  > **Note**: this field is always false and must not be used.
    @get:JsonProperty("AttachStdin")
    val attachStdin: kotlin.Boolean? = false,
    // Whether to attach to `stdout`.  <p><br /></p>  > **Note**: this field is always false and must not be used.
    @get:JsonProperty("AttachStdout")
    val attachStdout: kotlin.Boolean? = false,
    // Whether to attach to `stderr`.  <p><br /></p>  > **Note**: this field is always false and must not be used.
    @get:JsonProperty("AttachStderr")
    val attachStderr: kotlin.Boolean? = false,
    // An object mapping ports to an empty object in the form:  `{\"<port>/<tcp|udp|sctp>\": {}}`
    @get:JsonProperty("ExposedPorts")
    val exposedPorts: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,
    // Attach standard streams to a TTY, including `stdin` if it is not closed.  <p><br /></p>  > **Note**: this field is always false and must not be used.
    @get:JsonProperty("Tty")
    val tty: kotlin.Boolean? = false,
    // Open `stdin`  <p><br /></p>  > **Note**: this field is always false and must not be used.
    @get:JsonProperty("OpenStdin")
    val openStdin: kotlin.Boolean? = false,
    // Close `stdin` after one attached client disconnects.  <p><br /></p>  > **Note**: this field is always false and must not be used.
    @get:JsonProperty("StdinOnce")
    val stdinOnce: kotlin.Boolean? = false,
    // A list of environment variables to set inside the container in the form `[\"VAR=value\", ...]`. A variable without `=` is removed from the environment, rather than to have an empty value.
    @get:JsonProperty("Env")
    val env: kotlin.collections.List<kotlin.String>? = null,
    // Command to run specified as a string or an array of strings.
    @get:JsonProperty("Cmd")
    val cmd: kotlin.collections.List<kotlin.String>? = null,
    @get:JsonProperty("Healthcheck")
    val healthcheck: HealthConfig? = null,
    // Command is already escaped (Windows only)
    @get:JsonProperty("ArgsEscaped")
    val argsEscaped: kotlin.Boolean? = false,
    // The name (or reference) of the image to use when creating the container, or which was used when the container was created.  <p><br /></p>  > **Note**: this field is always empty and must not be used.
    @get:JsonProperty("Image")
    val image: kotlin.String? = "",
    // An object mapping mount point paths inside the container to empty objects.
    @get:JsonProperty("Volumes")
    val volumes: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,
    // The working directory for commands to run in.
    @get:JsonProperty("WorkingDir")
    val workingDir: kotlin.String? = null,
    // The entry point for the container as a string or an array of strings.  If the array consists of exactly one empty string (`[\"\"]`) then the entry point is reset to system default (i.e., the entry point used by docker when there is no `ENTRYPOINT` instruction in the `Dockerfile`).
    @get:JsonProperty("Entrypoint")
    val entrypoint: kotlin.collections.List<kotlin.String>? = null,
    // Disable networking for the container.  <p><br /></p>  > **Note**: this field is always omitted and must not be used.
    @get:JsonProperty("NetworkDisabled")
    val networkDisabled: kotlin.Boolean? = false,
    // MAC address of the container.  <p><br /></p>  > **Note**: this field is always omitted and must not be used.
    @get:JsonProperty("MacAddress")
    val macAddress: kotlin.String? = "",
    // `ONBUILD` metadata that were defined in the image's `Dockerfile`.
    @get:JsonProperty("OnBuild")
    val onBuild: kotlin.collections.List<kotlin.String>? = null,
    // User-defined key/value metadata.
    @get:JsonProperty("Labels")
    val labels: kotlin.collections.Map<kotlin.String, kotlin.String>? = null,
    // Signal to stop a container as a string or unsigned integer.
    @get:JsonProperty("StopSignal")
    val stopSignal: kotlin.String? = null,
    // Timeout to stop a container in seconds.  <p><br /></p>  > **Note**: this field is always omitted and must not be used.
    @get:JsonProperty("StopTimeout")
    val stopTimeout: kotlin.Int? = null,
    // Shell for when `RUN`, `CMD`, and `ENTRYPOINT` uses a shell.
    @get:JsonProperty("Shell")
    val shell: kotlin.collections.List<kotlin.String>? = null,
)
