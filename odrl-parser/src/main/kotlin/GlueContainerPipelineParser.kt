package fhg.glue.odrl

import com.fasterxml.jackson.databind.JsonNode

class GlueContainerPipelineParser : OdrlParser() {
    override val key: String = "glue-container-pipeline"

    fun parse(root: JsonNode): GlueContainerPipeline {
        if (!root.isObject) {
            throw OdrlException("Expected GLUE container pipeline to be an object!")
        }

        val containerNode = getFieldOrThrow(root, GlueOdrlIdentifiers.HAS_CONTAINER)

        val containers = mutableListOf<GlueContainer>()
        if (containerNode.isObject) {
            containers.add(GlueContainerParser().parse(containerNode))
        } else if (containerNode.isArray) {
            for (c in containerNode.elements())
                containers.add(GlueContainerParser().parse(c))
        } else {
            throw OdrlException("Expected field 'containers' to be a JSON object or array")
        }

        return GlueContainerPipeline(containers)
    }
}