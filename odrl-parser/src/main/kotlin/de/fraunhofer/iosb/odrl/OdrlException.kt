package de.fraunhofer.iosb.odrl

class OdrlException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)