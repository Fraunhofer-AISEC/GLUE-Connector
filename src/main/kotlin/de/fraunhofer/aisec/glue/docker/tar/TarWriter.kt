package de.fraunhofer.aisec.glue.docker.tar

import java.io.Closeable
import java.io.OutputStream

/**
 * The [TarWriter] class streams a standard USTAR archive to the provided output stream.
 * See https://pubs.opengroupo.org/onlinepubs/9699919799/utilities/pax.html#tag_20_92_13_06 for the specification.
 */
class TarWriter(private val stream: OutputStream) : Closeable {
    fun writeEntry(
        header: TarHeader,
        content: ByteArray,
    ) {
        assert(header.size == content.size) {
            "Mismatch between specified and actual size (header: ${header.size}, actual: ${content.size})"
        }

        stream.write(header.serialize())
        // Note: As per the standard, I think each 512 byte block should be written on its own
        // Since we are just writing the tar to a http stream to the docker daemon, we can relax this a bit
        stream.write(content)

        // Complete the last block
        val diff = content.size % 512
        if (diff != 0) {
            stream.write(ByteArray(512 - diff))
        }
    }

    fun writeFile(
        name: String,
        content: ByteArray,
    ) {
        writeEntry(TarHeader(name, content.size), content)
    }

    fun writeEndOfArchive() {
        stream.write(ByteArray(1024))
    }

    override fun close() {
        writeEndOfArchive()
        stream.close()
    }
}
