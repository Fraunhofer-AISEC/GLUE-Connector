package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.models.Contract

class PDP {
    /**
     * Check if a subject is allowed to call the specified HTTP endpoint under the given contract.
     */
    fun checkHttpCall(
        contract: Contract,
        subject: String,
        method: String,
        path: String,
    ): Boolean {
        // TODO
        return true
    }

    /**
     * Check if the contract allows starting the corresponding pipeline at this time.
     */
    fun checkStartPipeline(contract: Contract): Boolean {
        // TODO
        return true
    }
}
