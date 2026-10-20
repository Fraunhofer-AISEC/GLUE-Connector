/*
 * Copyright 2025 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
