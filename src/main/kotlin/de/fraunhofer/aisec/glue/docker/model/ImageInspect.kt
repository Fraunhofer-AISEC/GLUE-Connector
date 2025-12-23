package de.fraunhofer.aisec.glue.docker.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Information about an image in the local image cache.
 *
 * @param id ID is the content-addressable ID of an image.  This identifier is a content-addressable digest calculated from the image's configuration (which includes the digests of layers used by the image).  Note that this digest differs from the `RepoDigests` below, which holds digests of image manifests that reference the image.
 * @param repoTags List of image names/tags in the local image cache that reference this image.  Multiple image tags can refer to the same image, and this list may be empty if no tags reference the image, in which case the image is \"untagged\", in which case it can still be referenced by its ID.
 * @param repoDigests List of content-addressable digests of locally available image manifests that the image is referenced from. Multiple manifests can refer to the same image.  These digests are usually only available if the image was either pulled from a registry, or if the image was pushed to a registry, which is when the manifest is generated and its digest calculated.
 * @param parent ID of the parent image.  Depending on how the image was created, this field may be empty and is only set for images that were built/created locally. This field is empty if the image was pulled from an image registry.
 * @param comment Optional message that was set when committing or importing the image.
 * @param created Date and time at which the image was created, formatted in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format with nano-seconds.
 * @param container The ID of the container that was used to create the image.  Depending on how the image was created, this field may be empty.
 * @param containerConfig
 * @param dockerVersion The version of Docker that was used to build the image.  Depending on how the image was created, this field may be empty.
 * @param author Name of the author that was specified when committing the image, or as specified through MAINTAINER (deprecated) in the Dockerfile.
 * @param config
 * @param architecture Hardware CPU architecture that the image runs on.
 * @param variant CPU architecture variant (presently ARM-only).
 * @param os Operating System the image is built to run on.
 * @param osVersion Operating System version the image is built to run on (especially for Windows).
 * @param propertySize Total size of the image including all layers it is composed of.
 * @param graphDriver
 * @param rootFS
 * @param metadata
 */

data class ImageInspect(
    // ID is the content-addressable ID of an image.  This identifier is a content-addressable digest calculated from the image's configuration (which includes the digests of layers used by the image).  Note that this digest differs from the `RepoDigests` below, which holds digests of image manifests that reference the image.
    @get:JsonProperty("Id")
    val id: String? = null,
    // List of image names/tags in the local image cache that reference this image.  Multiple image tags can refer to the same image, and this list may be empty if no tags reference the image, in which case the image is \"untagged\", in which case it can still be referenced by its ID.
    @get:JsonProperty("RepoTags")
    val repoTags: List<String>? = null,
    // List of content-addressable digests of locally available image manifests that the image is referenced from. Multiple manifests can refer to the same image.  These digests are usually only available if the image was either pulled from a registry, or if the image was pushed to a registry, which is when the manifest is generated and its digest calculated.
    @get:JsonProperty("RepoDigests")
    val repoDigests: List<String>? = null,
    // ID of the parent image.  Depending on how the image was created, this field may be empty and is only set for images that were built/created locally. This field is empty if the image was pulled from an image registry.
    @get:JsonProperty("Parent")
    val parent: String? = null,
    // Optional message that was set when committing or importing the image.
    @get:JsonProperty("Comment")
    val comment: String? = null,
    // Date and time at which the image was created, formatted in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format with nano-seconds.
    @get:JsonProperty("Created")
    val created: String? = null,
    // The ID of the container that was used to create the image.  Depending on how the image was created, this field may be empty.
    @get:JsonProperty("Container")
    val container: String? = null,
    @get:JsonProperty("ContainerConfig")
    val containerConfig: ContainerConfig? = null,
    // The version of Docker that was used to build the image.  Depending on how the image was created, this field may be empty.
    @get:JsonProperty("DockerVersion")
    val dockerVersion: String? = null,
    // Name of the author that was specified when committing the image, or as specified through MAINTAINER (deprecated) in the Dockerfile.
    @get:JsonProperty("Author")
    val author: String? = null,
    @get:JsonProperty("Config")
    val config: ImageConfig? = null,
    // Hardware CPU architecture that the image runs on.
    @get:JsonProperty("Architecture")
    val architecture: String? = null,
    // CPU architecture variant (presently ARM-only).
    @get:JsonProperty("Variant")
    val variant: String? = null,
    // Operating System the image is built to run on.
    @get:JsonProperty("Os")
    val os: String? = null,
    // Operating System version the image is built to run on (especially for Windows).
    @get:JsonProperty("OsVersion")
    val osVersion: String? = null,
    // Total size of the image including all layers it is composed of.
    @get:JsonProperty("Size")
    val propertySize: Long? = null,
    @get:JsonProperty("GraphDriver")
    val graphDriver: GraphDriverData? = null,
    @get:JsonProperty("RootFS")
    val rootFS: ImageInspectRootFS? = null,
    @get:JsonProperty("Metadata")
    val metadata: ImageInspectMetadata? = null,
)
