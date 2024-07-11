package de.fraunhofer.aisec.glue

object Utils {
    const val HEX_DIGITS = "0123456789abcdef"

    fun hexEncode(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val int = byte.toInt() and 0xff
            builder.append(HEX_DIGITS[int shr 4])
            builder.append(HEX_DIGITS[int and 0x0f])
        }
        return builder.toString()
    }
}
