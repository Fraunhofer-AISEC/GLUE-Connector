package fhg.glue.odrl

class OdrlAction(val name: String)
{
    fun matches(ctx: Context): Boolean {
        return ctx.actionName == name
    }

    fun exercise(ctx: Context): Boolean {
        return true;
    }
}