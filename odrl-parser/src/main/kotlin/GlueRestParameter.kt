package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class GlueRestParameter(
    val url: String,
    val httpType: String,
    val httpBody: JsonNode? = null) : GlueParameter
{
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GlueRestParameter)
            return false;
        return other.url == url
                && other.httpType == httpType
                && other.httpBody == httpBody
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + httpType.hashCode()
        result = 31 * result + (httpBody?.hashCode() ?: 0)
        return result
    }
}