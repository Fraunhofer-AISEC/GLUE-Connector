package fhg.glue.odrl

class OdrlOperator(val operator: String)
{
    fun eval(ctx: Context, leftOperand: OdrlLeftOperand, rightOperand: OdrlLiteral<*>): Boolean {
        when (operator) {
            OdrlIdentifiers.EQ -> return leftOperand.eval(ctx) == rightOperand
            OdrlIdentifiers.NEQ -> return leftOperand.eval(ctx) != rightOperand
            OdrlIdentifiers.GT -> return leftOperand.eval(ctx) > rightOperand
            OdrlIdentifiers.GTEQ -> return leftOperand.eval(ctx) >= rightOperand
            OdrlIdentifiers.LT -> return leftOperand.eval(ctx) < rightOperand
            OdrlIdentifiers.LTEQ -> return leftOperand.eval(ctx) <= rightOperand
            OdrlIdentifiers.HASPART -> return leftOperand.eval(ctx).hasPart(rightOperand)
            OdrlIdentifiers.ISPARTOF -> return leftOperand.eval(ctx).isPartOf(rightOperand)
            OdrlIdentifiers.ISA -> throw OdrlException("ODRL operator isA not supported")
            OdrlIdentifiers.ISALLOF -> throw OdrlException("ODRL operator isAllOf not supported")
            OdrlIdentifiers.ISANYOF -> throw OdrlException("ODRL operator isAnyOf not supported")
            OdrlIdentifiers.ISNONEOF -> throw OdrlException("ODRL operator isNoneOf not supported")
        }
        throw OdrlException("Invalid ODRL operator $operator")
    }
}