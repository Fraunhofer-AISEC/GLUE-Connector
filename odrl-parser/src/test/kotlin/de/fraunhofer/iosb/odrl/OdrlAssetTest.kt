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

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlAssetTest {
    private val mapper = Commons.parserInstance.mapper
    private val asset1 =
        OdrlAsset(
            uid = URI.create("http://example.com/asset"),
            partOf =
                listOf(
                    OdrlAssetCollection(
                        source = OdrlIri("http://example.com/assetCollection"),
                        refinement =
                            listOf(
                                OdrlConstraint(
                                    leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                                    operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                                    rightOperand = listOf(PrimitiveString("exampleRightOperand")),
                                ),
                            ),
                    ),
                ),
        )

    @Test
    fun testAssetDeserialization() {
        val asset = mapper.readValue(loadFileOrDie("/asset-example-1.json"), OdrlAsset::class.java)
        assertEquals(asset1, asset)
    }

    @Test
    fun testAssetSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(asset1)
        val asset = mapper.readValue(string, OdrlAsset::class.java)
        assertEquals(asset1, asset)
    }

    @Test
    fun testAssetDeserializationWithInvalidUri() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"Asset\",\n" +
                    "  \"@id\": \":::invalid-uri:::\"\n" +
                    "}",
                OdrlAsset::class.java,
            )
        }
    }
}
