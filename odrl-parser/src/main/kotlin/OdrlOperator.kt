package fhg.glue.odrl

class OdrlOperator(val operator: String)
{
    fun eval(leftOperand: OdrlLeftOperand, rightOperand: OdrlLiteral<*>): Boolean {
        when (operator) {
            OdrlIdentifiers.EQ -> return leftOperand.eval() == rightOperand
            OdrlIdentifiers.NEQ -> return leftOperand.eval() != rightOperand
            OdrlIdentifiers.GT -> return leftOperand.eval() > rightOperand
            OdrlIdentifiers.GTEQ -> return leftOperand.eval() >= rightOperand
            OdrlIdentifiers.LT -> return leftOperand.eval() < rightOperand
            OdrlIdentifiers.LTEQ -> return leftOperand.eval() <= rightOperand
            OdrlIdentifiers.HASPART -> return leftOperand.eval().hasPart(rightOperand)
            OdrlIdentifiers.ISPARTOF -> return leftOperand.eval().isPartOf(rightOperand)
            OdrlIdentifiers.ISA -> throw OdrlException("ODRL operator isA not supported")
            OdrlIdentifiers.ISALLOF -> throw OdrlException("ODRL operator isAllOf not supported")
            OdrlIdentifiers.ISANYOF -> throw OdrlException("ODRL operator isAnyOf not supported")
            OdrlIdentifiers.ISNONEOF -> throw OdrlException("ODRL operator isNoneOf not supported")
        }
        throw OdrlException("Invalid ODRL operator $operator")
    }
}