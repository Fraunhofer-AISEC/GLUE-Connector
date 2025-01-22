package de.fraunhofer.aisec.glue.policy

class PolicyViolationException(message: String) : Exception(message) {
    constructor(message: String, causes: Iterable<Throwable>) : this(message) {
        causes.forEach(this::addSuppressed)
    }
}
