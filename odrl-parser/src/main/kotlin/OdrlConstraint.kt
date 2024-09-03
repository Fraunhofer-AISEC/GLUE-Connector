package fhg.glue.odrl

class OdrlConstraint(
    val uid: String?,
    val leftOperand: OdrlLeftOperand,
    val operator: OdrlOperator,
    val rightOperand: OdrlLiteral<*>)
{
    fun isSatisfied(ctx: Context): Boolean {
        return operator.eval(ctx, leftOperand, rightOperand)
    }
}