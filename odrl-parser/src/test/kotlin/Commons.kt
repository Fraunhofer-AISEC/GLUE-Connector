import de.fraunhofer.iosb.odrl.parser.OdrlParser

class Commons {
    companion object {
        // Use a singleton parser instance to share the document loader cache over all tests
        // This prevents HTTP 429 errors (too many requests)
        val parserInstance: OdrlParser = OdrlParser()
    }
}