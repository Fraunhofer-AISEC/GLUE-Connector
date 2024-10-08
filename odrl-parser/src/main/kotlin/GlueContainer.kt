package fhg.glue.odrl

class GlueContainer(
    val image: String,
    val hash: String,
    val parameters: List<GlueParameter> = listOf(),
    val allowNetwork: Boolean = false)
{
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GlueContainer)
            return false;
        return other.image == image
                && other.hash == hash
                && other.parameters == parameters
                && other.allowNetwork == allowNetwork
    }

    override fun toString(): String {
        return "$image[$hash]"
    }

    override fun hashCode(): Int {
        var result = image.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + allowNetwork.hashCode()
        return result
    }
}