package de.fraunhofer.aisec.glue

class PDP {
    /**
     * Check if a subject is allowed to call the specified HTTP endpoint under the given contract (TODO: Proper type).
     */
    fun checkHttpCall(
        contract: String,
        subject: String,
        method: String,
        path: String,
    ): Boolean {
        // TODO
        return true
    }
}
