package de.fraunhofer.aisec.glue

object Utils {
    private const val HEX_DIGITS = "0123456789abcdef"

    fun ByteArray.hexEncode(): String {
        val builder = StringBuilder(size * 2)
        for (byte in this) {
            val int = byte.toInt() and 0xff
            builder.append(HEX_DIGITS[int shr 4])
            builder.append(HEX_DIGITS[int and 0x0f])
        }
        return builder.toString()
    }

    fun <T> identity(value: T) = value
}
