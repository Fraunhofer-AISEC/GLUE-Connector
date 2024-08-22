package fhg.glue.odrl

class OdrlAction(
    val uid: String,
    val refinements: List<OdrlConstraint>)
{
    fun matches(ctx: Context): Boolean {
        if (ctx.actionUid != uid)
            return false

        // Refinements have to be satisfied
        for (r in refinements) {
            if (!r.isSatisfied(ctx))
                return false;
        }

        return true
    }

    fun exercise(ctx: Context, onExerciseAction: (OdrlAction) -> Boolean): Boolean {
        return onExerciseAction(this);
    }
}