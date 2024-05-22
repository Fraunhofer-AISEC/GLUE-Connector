package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.models.Contract
import java.security.SecureRandom
import kotlin.collections.set

class ContractManager {
    // TODO: Some persisted storage
    private val contracts = mutableMapOf<String, Contract>()

    fun get(contractId: String): Contract? {
        return contracts[contractId]
    }

    private fun generateContractId(): String {
        val bytes = ByteArray(16)
        SecureRandom.getInstanceStrong().nextBytes(bytes)
        return hexEncode(bytes)
    }

    fun register(contract: Contract): String {
        // TODO: Synchronize this as there can technically be a race condition between multiple coroutines
        val contractId =
            generateSequence { generateContractId() }
                .first { !contracts.containsKey(it) }

        contracts[contractId] = contract
        return contractId
    }

    fun deregister(contractId: String) {
        contracts.remove(contractId)
    }

    companion object {
        private const val HEX_DIGITS = "0123456789abcdef"

        private fun hexEncode(bytes: ByteArray): String {
            val builder = StringBuilder(bytes.size * 2)
            for (byte in bytes) {
                val int = byte.toInt() and 0xff
                builder.append(HEX_DIGITS[int shr 4])
                builder.append(HEX_DIGITS[int and 0x0f])
            }
            return builder.toString()
        }
    }
}
