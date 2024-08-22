package fhg.glue.odrl

class OdrlAsset(val uid: String)
{
    fun matches(ctx: Context): Boolean {
        return ctx.targetUid == uid
    }
}