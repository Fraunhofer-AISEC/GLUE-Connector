package fhg.glue.odrl

open class OdrlRule(
    val uid: String?,
    val action: OdrlAction,
    val constraints: List<OdrlConstraint>)
{
}