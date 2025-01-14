package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlAssetCollectionTest {
    private val mapper = Commons.parserInstance.mapper
    private val assetCollection1 = OdrlAssetCollection(
        source = OdrlIri("http://example.com/assetCollection"),
        refinement = listOf(
            OdrlConstraint(
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand1"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                rightOperand = listOf(PrimitiveString("exampleRightOperand1"))
            ),
            OdrlConstraint(
                leftOperand = OdrlLeftOperand("http://example.com/leftOperand2"),
                operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                rightOperand = listOf(PrimitiveString("exampleRightOperand2"))
            )
        )
    )

    @Test
    fun testAssetCollectionDeserialization() {
        val assetCollection = mapper.readValue(loadFileOrDie("/asset-collection-example-1.json"), OdrlAssetCollection::class.java)
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
                        "}", OdrlAssetCollection::class.java
            )
        }
    }
}