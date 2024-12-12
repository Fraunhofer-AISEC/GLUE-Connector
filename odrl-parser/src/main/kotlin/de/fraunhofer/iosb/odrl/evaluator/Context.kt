package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import java.net.URI

data class Context(
    val params: MutableMap<String, Any?>
) {
    constructor(
        actionUid: String,
        targetUid: String,
        currentPipeline: GlueContainerPipeline? = null
    ): this(URI.create(actionUid), URI.create(targetUid), currentPipeline)

    constructor(
        actionUid: URI,
        targetUid: URI,
        currentPipeline: GlueContainerPipeline? = null
    ): this(mutableMapOf()) {
        params["actionUid"] = actionUid
        params["targetUid"] = targetUid
        params["pipeline"] = currentPipeline
    }
}