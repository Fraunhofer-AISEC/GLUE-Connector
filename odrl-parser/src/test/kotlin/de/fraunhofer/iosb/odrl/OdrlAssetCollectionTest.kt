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
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlAssetCollectionTest {
    private val mapper = Commons.parserInstance.mapper
    private val assetCollection1 =
        OdrlAssetCollection(
            source = OdrlIri("http://example.com/assetCollection"),
            refinement =
                listOf(
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand("http://example.com/leftOperand1"),
                        operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                        rightOperand = listOf(PrimitiveString("exampleRightOperand1")),
                    ),
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand("http://example.com/leftOperand2"),
                        operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                        rightOperand = listOf(PrimitiveString("exampleRightOperand2")),
                    ),
                ),
        )

    @Test
    fun testAssetCollectionDeserialization() {
        val assetCollection =
            mapper.readValue(loadFileOrDie("/asset-collection-example-1.json"), OdrlAssetCollection::class.java)
        assertEquals(assetCollection1, assetCollection)
    }

    @Test
    fun testAssetCollectionSerializationAndDeserialization() {
        val string = mapper.writeValueAsString(assetCollection1)
        val assetCollection = mapper.readValue(string, OdrlAssetCollection::class.java)
        assertEquals(assetCollection1, assetCollection)
    }

    @Test
    fun testAssetCollectionDeserializationWithInvalidSource() {
        assertThrows<InvalidFormatException> {
            mapper.readValue(
                "{\n" +
                    "  \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
                    "  \"@type\": \"AssetCollection\",\n" +
                    "  \"source\": \":::invalid-uri:::\"\n" +
                    "}",
                OdrlAssetCollection::class.java,
            )
        }
    }
}
