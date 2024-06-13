package fhg.glue.odrl

class OdrlOperator(val operator: String)
{
    fun eval(leftOperand: OdrlLeftOperand, rightOperand: OdrlLiteral<*>): Boolean {
        when (operator) {
            "eq" -> return leftOperand.eval() == rightOperand
            "neq" -> return leftOperand.eval() != rightOperand
            "gt" -> return leftOperand.eval() > rightOperand
            "gteq" -> return leftOperand.eval() >= rightOperand
            "lt" -> return leftOperand.eval() < rightOperand
            "lteq" -> return leftOperand.eval() <= rightOperand
            "hasPart" -> return leftOperand.eval().hasPart(rightOperand)
            "isPartOf" -> return leftOperand.eval().isPartOf(rightOperand)
            "isA" -> throw OdrlException("ODRL operator isA not supported")
            "isAllOf" -> throw OdrlException("ODRL operator isAllOf not supported")
            "isAnyOf" -> throw OdrlException("ODRL operator isAnyOf not supported")
            "isNoneOf" -> throw OdrlException("ODRL operator isNoneOf not supported")
        }
        throw OdrlException("Invalid ODRL operator $operator")
    }
}