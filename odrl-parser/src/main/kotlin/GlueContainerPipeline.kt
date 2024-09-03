package fhg.glue.odrl

class GlueContainerPipeline(
    val  containers: List<GlueContainer>)
{
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is GlueContainerPipeline)
            return false;
        return other.containers == containers;
    }

    override fun toString(): String {
        return containers.joinToString(prefix = "(", postfix = ")", separator = " -> ")
    }

}