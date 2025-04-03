package de.fraunhofer.iosb.odrl

import de.fraunhofer.iosb.odrl.parser.OdrlParser
import java.io.BufferedReader
import kotlin.system.exitProcess

class Commons {
    companion object {
        // Use singleton parser instances to share the document loader cache over all tests
        // This prevents HTTP 429 errors (too many requests)
        val parserInstance: OdrlParser = OdrlParser()
        val compactParserInstance: OdrlParser = OdrlParser(compactSerialization = true)

        fun loadFileOrDie(fileName: String): BufferedReader {
            val reader = {}.javaClass.getResourceAsStream(fileName)?.bufferedReader()
            if (reader == null) {
                println("Failed to load file $fileName")
                exitProcess(-1)
            }
            return reader
        }
    }
}