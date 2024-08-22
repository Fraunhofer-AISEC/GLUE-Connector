package fhg.glue.odrl

class OdrlDuty(
    uid: String?,
    action: OdrlAction,
    constraints: List<OdrlConstraint>,
    val target: OdrlAsset?,
    val assigner: OdrlParty?,
    val assignee: OdrlParty?) : OdrlRule(uid, action, constraints)
{
    fun isFulfilled(ctx: Context, onExerciseAction: (OdrlAction) -> Boolean): Boolean {
        // Target has to match
        if (target != null && !target.matches(ctx))
            return false;

        // Constraints have to be satisfied
        for (c in constraints) {
            if (!c.isSatisfied(ctx))
                return false;
        }

        // Action has to be exercised
        return action.exercise(ctx, onExerciseAction);
    }
}