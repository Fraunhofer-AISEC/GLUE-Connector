package de.fraunhofer.aisec.glue.pipeline

@JvmInline
value class IPv4Address(val value: UInt) {
    override fun toString(): String {
        return "${(value shr 24) and 0xffu}.${(value shr 16) and 0xffu}.${(value shr 8) and 0xffu}.${value and 0xffu}"
    }

    fun networkAddress(prefixLength: Int): IPv4Address {
        if (!(0..32).contains(prefixLength)) {
            throw IllegalArgumentException("IP prefix out of range")
        }
        return IPv4Address(value and (-1 shl (32 - prefixLength)).toUInt())
    }

    companion object {
        fun parse(encoded: String): IPv4Address {
            val segments = encoded.split('.').map(String::toUInt)
            if (segments.size != 4) {
                throw IllegalArgumentException("IPv4 address must consist of four segments")
            }
            if (segments.any { s -> s > 255u }) {
                throw IllegalArgumentException("All IPv4 address segments must be between 1 and 255")
            }
            return IPv4Address(segments.fold(0u) { a, s -> (a shl 8) or s })
        }
    }
}
