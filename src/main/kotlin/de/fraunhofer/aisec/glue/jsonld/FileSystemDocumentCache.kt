package de.fraunhofer.aisec.glue.jsonld

import com.apicatalog.jsonld.JsonLdError
import com.apicatalog.jsonld.document.Document
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.document.RdfDocument
import com.apicatalog.jsonld.http.media.MediaType
import com.apicatalog.jsonld.loader.DocumentLoader
import com.apicatalog.jsonld.loader.DocumentLoaderOptions
import com.apicatalog.rdf.Rdf
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.io.IOException
import java.io.StringWriter
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * A [com.apicatalog.jsonld.loader.DocumentLoader] that caches JSON-LD documents on disk.
 * On a cache miss, the delegate loader is called and the result cached to [cacheDir].
 */
class FileSystemDocumentCache(
    private val delegate: DocumentLoader,
    private val cacheDir: Path,
    private val mapper: ObjectMapper,
) : DocumentLoader {
    @Throws(JsonLdError::class)
    override fun loadDocument(
        url: URI,
        options: DocumentLoaderOptions,
    ): Document {
        val cacheFile = cacheDir.resolve(urlToFilename(url))

        if (Files.exists(cacheFile)) {
            LOG.atTrace {
                message = "JSON-LD disk cache hit"
                payload = mapOf("url" to url.toString(), "cacheFile" to cacheFile.toString())
            }
            return loadFromDisk(cacheFile)
        }

        LOG.atTrace {
            message = "JSON-LD disk cache miss, delegating to the next loader"
            payload = mapOf("url" to url.toString(), "cacheFile" to cacheFile.toString())
        }
        val remoteDocument = delegate.loadDocument(url, options)

        try {
            saveToDisk(cacheFile, remoteDocument)
        } catch (e: Exception) {
            LOG.atWarn {
                message = "Failed to write JSON-LD document to disk cache: $url"
                cause = e
                payload = mapOf("url" to url.toString(), "cacheFile" to cacheFile.toString())
            }
        }

        return remoteDocument
    }

    private fun urlToFilename(url: URI): String {
        val hash =
            MessageDigest
                .getInstance("SHA-1")
                .digest(url.toString().toByteArray())
                .toHexString()
        return "$hash.json"
    }

    private fun loadFromDisk(path: Path): Document {
        Files.newBufferedReader(path).use { reader ->
            val wrapper = mapper.readValue<CachedDocumentWrapper>(reader)
            val mediaType = MediaType.of(wrapper.contentType)
            val contentStream = wrapper.content.byteInputStream()

            return when {
                JsonDocument.accepts(mediaType) -> JsonDocument.of(mediaType, contentStream)
                RdfDocument.accepts(mediaType) -> RdfDocument.of(mediaType, contentStream)
                else -> throw IllegalStateException("Unsupported media type: $mediaType")
            }.apply {
                documentUrl = wrapper.documentUrl
                contextUrl = wrapper.contextUrl
            }
        }
    }

    private fun saveToDisk(
        path: Path,
        document: Document,
    ) {
        Files.createDirectories(path.parent)

        val content =
            when (document) {
                is JsonDocument -> document.jsonContent.orElseThrow().toString()
                // TODO: How to properly serialize RDF documents?
                is RdfDocument ->  {
                    val buffer = StringWriter()
                    Rdf.createWriter(document.contentType, buffer).write(document.rdfContent.orElseThrow())
                    buffer.toString()
                }
                else -> throw IllegalStateException("Unsupported document type: ${document::class.simpleName}")
            }
        val wrapper =
            CachedDocumentWrapper(
                documentUrl = document.documentUrl,
                contextUrl = document.contextUrl,
                contentType = document.contentType.toString(),
                content = content,
            )

        val tmpFile = Files.createTempFile(path.parent, ".tmp-", ".json")
        try {
            Files.newBufferedWriter(tmpFile).use { writer ->
                mapper.writeValue(writer, wrapper)
            }
            Files.move(tmpFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            Files.deleteIfExists(tmpFile)
            throw e
        }
    }

    private class CachedDocumentWrapper(
        val documentUrl: URI?,
        val contextUrl: URI?,
        val contentType: String?,
        val content: String,
    )

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
