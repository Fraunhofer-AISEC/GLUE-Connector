package de.fraunhofer.iosb.odrl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.fraunhofer.iosb.odrl.Commons.Companion.loadFileOrDie
import org.junit.jupiter.api.assertThrows
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class OdrlAssetTest {
    private val mapper = Commons.parserInstance.mapper
    private val asset1 = OdrlAsset(
        uid = URI.create("http://example.com/asset"),
        partOf = listOf(
            OdrlAssetCollection(
                source = OdrlIri("http://example.com/assetCollection"),
                refinement = listOf(
                    OdrlConstraint(
                        leftOperand = OdrlLeftOperand("http://example.com/leftOperand"),
                        operator = OdrlOperator("http://www.w3.org/ns/odrl/2/eq"),
                        rightOperand = listOf(PrimitiveString("exampleRightOperand"))
                    )
                )
            )
        )
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
                        "}", OdrlAsset::class.java
            )
        }
    }
}