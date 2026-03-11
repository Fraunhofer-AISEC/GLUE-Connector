package de.fraunhofer.aisec.glue

object Constants {
    const val TC_USER_AGENT = "TrustedConnector/1.0.0"
    val TC_USER_AGENT_REGEX = """TrustedConnector/\d+(?:\.\d+)*""".toRegex()
}
