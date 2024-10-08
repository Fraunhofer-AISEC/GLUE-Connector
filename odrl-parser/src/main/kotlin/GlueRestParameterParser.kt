package fhg.glue.odrl

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class GlueRestParameterParser : OdrlParser() {
    override val key: String = "glue-rest-parameter"

    fun parse(root: JsonNode): GlueRestParameter {
        if (!root.isObject) {
            throw OdrlException("Expected GLUE REST parameter to be an object!")
        }

        val url = getStringOrThrow(root, GlueOdrlIdentifiers.URL)
        val httpType = getStringOrThrow(root, GlueOdrlIdentifiers.HTTP_TYPE).uppercase()
        if (httpType != "GET" && httpType != "POST")
            throw OdrlException("Expected GLUE REST parameter to have httpType GET or POST!")

        var httpBody: JsonNode? = null;
        if (root.has(GlueOdrlIdentifiers.HTTP_BODY)) {
            val httpBodyString = root.get(GlueOdrlIdentifiers.HTTP_BODY).asText()
            try {
                httpBody = ObjectMapper().readTree(httpBodyString)
            } catch (e: JsonParseException) {
                throw OdrlException("Failed to parse JSON in GLUE REST parameter!", e)
            } catch (e: JsonProcessingException) {
                throw OdrlException("Failed to process JSON in GLUE REST parameter!", e)
            }
        }

        return GlueRestParameter(url, httpType, httpBody)
    }
}