package fhg.glue.odrl

class GlueContainer(
    val image: String,
    val hash: String,
    val entrypoint: String? = null,
    val allowNetwork: Boolean = false)
{
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GlueContainer)
            return false;
        return other.image == image
                && other.hash == hash
                && other.entrypoint == entrypoint
                && other.allowNetwork == allowNetwork
    }

    override fun toString(): String {
        return "$image[entrypoint='$entrypoint']"
    }
}