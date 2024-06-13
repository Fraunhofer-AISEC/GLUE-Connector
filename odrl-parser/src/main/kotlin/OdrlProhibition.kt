package fhg.glue.odrl

class OdrlProhibition(
    uid: String?,
    action: OdrlAction,
    constraints: List<OdrlConstraint>,
    val target: OdrlAsset?,
    val assigner: OdrlParty?,
    val assignee: OdrlParty?,
    val remedies: List<OdrlDuty>) : OdrlRule(uid, action, constraints)
{
    fun isDisallowed(ctx: Context): Boolean {
        // Action has to match
        if(!action.matches(ctx))
            return false;

        // Target has to match
        if (target != null && !target.matches(ctx))
            return false;

        // Constraints have to be satisfied
        for (c in constraints) {
            if (!c.isSatisfied(ctx))
                return false;
        }


        return true; // This prohibition disallows context |ctx|
    }
}