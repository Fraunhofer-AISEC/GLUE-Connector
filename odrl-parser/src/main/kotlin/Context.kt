package fhg.glue.odrl

class Context(val params: MutableMap<String, Any?>) {
    constructor(actionUid: String, targetUid: String, pipeline: GlueContainerPipeline? = null) : this(mutableMapOf()) {
        params["actionUid"] = actionUid;
        params["targetUid"] = targetUid;
        params["pipeline"] = pipeline;
    }
}