package de.fraunhofer.iosb.odrl.evaluator

data class Result(
    val success: Boolean,
    val failureReasoning: MutableList<String> = mutableListOf(),
) {
    init {
        if (success && failureReasoning.isNotEmpty())
            throw IllegalArgumentException("Successful result cannot have failure reasons")
    }

    constructor(
        success: Boolean,
        failureReason: String,
    ): this(success, if (success) mutableListOf() else mutableListOf(failureReason))

    constructor(
        result: Result,
        failureReason: String,
    ): this(result.success, result.failureReasoning) {
        if (!this.success)
            this.failureReasoning.add(0, failureReason)
    }
}