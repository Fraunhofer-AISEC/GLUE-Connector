package fhg.glue.odrl

class OdrlPolicy(
    val uid: String,
    val permissions: List<OdrlPermission>,
    val prohibitions: List<OdrlProhibition>,
    val obligations: List<OdrlDuty>,
    val profiles: List<String>,
    val conflict: String)
{
    fun evaluate(ctx: Context): Boolean {
        // Evaluate permissions
        var allowed = false
        for (p in permissions)
            allowed = allowed || p.isAllowed(ctx)

        // Evaluate prohibitions
        var disallowed = false;
        for (p in prohibitions)
            disallowed = disallowed || p.isDisallowed(ctx)

        // Resolve conflicts
        if (allowed && disallowed) {
            if (conflict == "invalid") throw OdrlException("Policy is conflicted: Cannot both prohibit and permit!")
            if (conflict == "perm") disallowed = false
            if (conflict == "prohibit") allowed = false
        }
        var result = allowed && !disallowed

        // Exercise obligations
        if (result)
            for (o in obligations)
                result = result && o.isFulfilled(ctx)

        return result;
    }
}