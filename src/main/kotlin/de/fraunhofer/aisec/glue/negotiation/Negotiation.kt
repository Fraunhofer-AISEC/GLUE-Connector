package de.fraunhofer.aisec.glue.negotiation

interface Negotiation {
    /**
     * Cancel this negotiation
     */
    suspend fun cancel(cause: String?)
}