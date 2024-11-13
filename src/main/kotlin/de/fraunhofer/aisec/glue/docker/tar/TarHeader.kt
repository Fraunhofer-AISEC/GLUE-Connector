package de.fraunhofer.aisec.glue.docker.tar

import kotlin.math.min

data class TarHeader(
    val name: String,
    val size: Int,
    // 0644 in octal, which kotlin does not support
    val mode: Int = 420,
    val uid: Int = 0,
    val gid: Int = 0,
    val mtime: Int = 0,
    val typeFlag: Byte = TypeFlag.REGULAR,
    val linkName: String = "",
    val uname: String = "",
    val gname: String = "",
    val devMajor: Int = 0,
    val devMinor: Int = 0,
    val prefix: String = "",
) {
    private fun writeString(
        buffer: ByteArray,
        string: String,
        offset: Int,
        maxLength: Int,
    ) {
        val byteString = string.toByteArray(Charsets.US_ASCII)
        val length = min(maxLength, byteString.size)
        System.arraycopy(byteString, 0, buffer, offset, length)
    }

    private fun writeInt(
        buffer: ByteArray,
        int: Int,
        offset: Int,
        maxLength: Int,
    ) {
        val octalString = int.toString(8).padStart(maxLength - 1, '0')
        if (octalString.length > maxLength - 1) {
            throw IllegalArgumentException("Number is too large for the tar format")
        }
        writeString(buffer, octalString, offset, maxLength - 1)
        // Ensure the null terminator is present
        buffer[offset + maxLength - 1] = 0
    }

    fun serialize(): ByteArray {
        val buffer = ByteArray(512)

        writeString(buffer, name, 0, 100)
        writeInt(buffer, mode, 100, 8)
        writeInt(buffer, uid, 108, 8)
        writeInt(buffer, gid, 116, 8)
        writeInt(buffer, size, 124, 12)
        writeInt(buffer, mtime, 136, 12)
        // The checksum field is initialized to all spaces as per the spec
        writeString(buffer, " ".repeat(8), 148, 8)
        writeString(buffer, typeFlag.toString(8), 156, 1)
        writeString(buffer, linkName, 157, 100)
        writeString(buffer, "ustar", 257, 6)
        writeString(buffer, "00", 263, 2)
        // Note: We specify a max len one shorter than the specification to guarantee a null terminator
        writeString(buffer, uname, 265, 31)
        writeString(buffer, gname, 297, 31)
        writeInt(buffer, devMajor, 329, 8)
        writeInt(buffer, devMinor, 337, 8)
        writeString(buffer, prefix, 348, 155)

        val checksum = buffer.map(Byte::toUInt).reduce(UInt::plus)
        writeInt(buffer, checksum.toInt(), 148, 8)

        return buffer
    }

    object TypeFlag {
        const val REGULAR: Byte = 0
        const val LINK: Byte = 1
        const val SYMLINK: Byte = 2
        const val CHAR_DEVICE: Byte = 3
        const val BLOCK_DEVICE: Byte = 4
        const val DIRECTORY: Byte = 5
        const val FIFO: Byte = 6
        const val CUSTOM: Byte = 7
    }
}
