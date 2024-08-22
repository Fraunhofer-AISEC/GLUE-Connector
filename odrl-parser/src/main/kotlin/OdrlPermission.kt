package fhg.glue.odrl

class OdrlPermission(
    uid: String?,
    action: OdrlAction,
    constraints: List<OdrlConstraint>,
    val target: OdrlAsset?,
    val assigner: OdrlParty?,
    val assignee: OdrlParty?,
    val duties: List<OdrlDuty>) : OdrlRule(uid, action, constraints)
{
    fun isAllowed(ctx: Context, onExerciseAction: (OdrlAction) -> Boolean): Boolean {
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

        // Duties have to be fulfilled
        for (d in duties) {
            if (!d.isFulfilled(ctx, onExerciseAction))
                return false;
        }

        return true; // This permission allows context |ctx|
    }
}