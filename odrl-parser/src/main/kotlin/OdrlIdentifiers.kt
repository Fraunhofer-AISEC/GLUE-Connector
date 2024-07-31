package fhg.glue.odrl

object OdrlIdentifiers {
    // ODRL vocabulary: https://www.w3.org/TR/odrl-vocab

    // 3.1) Policy
    const val POLICY = "http://www.w3.org/ns/odrl/2/Policy"
    const val UID = "http://www.w3.org/ns/odrl/2/uid"
    const val PROFILE = "http://www.w3.org/ns/odrl/2/profile"

    // 3.2) Policy subclasses
    const val SET = "http://www.w3.org/ns/odrl/2/Set"

    // 3.5) Asset Relations
    const val TARGET = "http://www.w3.org/ns/odrl/2/target"

    // 3.7) Party Functions
    const val ASSIGNEE = "http://www.w3.org/ns/odrl/2/assignee"
    const val ASSIGNER = "http://www.w3.org/ns/odrl/2/assigner"

    // 3.9) Permission
    const val PERMISSION = "http://www.w3.org/ns/odrl/2/Permission"
    const val HAS_PERMISSION = "http://www.w3.org/ns/odrl/2/permission"

    // 3.10) Prohibition
    const val PROHIBITION = "http://www.w3.org/ns/odrl/2/Prohibition"
    const val HAS_PROHIBITION = "http://www.w3.org/ns/odrl/2/prohibition"

    // 3.11) Action
    const val ACTION = "http://www.w3.org/ns/odrl/2/Action"
    const val HAS_ACTION = "http://www.w3.org/ns/odrl/2/action"

    // 3.13) Duty
    const val DUTY = "http://www.w3.org/ns/odrl/2/Duty"
    const val OBLIGATION = "http://www.w3.org/ns/odrl/2/obligation"
    const val HAS_DUTY = "http://www.w3.org/ns/odrl/2/duty"
    const val REMEDY = "http://www.w3.org/ns/odrl/2/remedy"

    // 3.14) Constraint
    const val CONSTRAINT = "http://www.w3.org/ns/odrl/2/Constraint"
    const val HAS_CONSTRAINT = "http://www.w3.org/ns/odrl/2/constraint"
    const val REFINEMENT = "http://www.w3.org/ns/odrl/2/refinement"
    const val OPERATOR = "http://www.w3.org/ns/odrl/2/Operator"
    const val HAS_OPERATOR = "http://www.w3.org/ns/odrl/2/operator"
    const val RIGHTOPERAND = "http://www.w3.org/ns/odrl/2/RightOperand"
    const val HAS_RIGHTOPERAND = "http://www.w3.org/ns/odrl/2/rightOperand"
    const val LEFTOPERAND = "http://www.w3.org/ns/odrl/2/LeftOperand"
    const val HAS_LEFTOPERAND = "http://www.w3.org/ns/odrl/2/leftOperand"

    // 3.16) Constraint Operators
    const val EQ = "http://www.w3.org/ns/odrl/2/eq"
    const val GT = "http://www.w3.org/ns/odrl/2/gt"
    const val GTEQ = "http://www.w3.org/ns/odrl/2/gteq"
    const val LT = "http://www.w3.org/ns/odrl/2/lt"
    const val LTEQ = "http://www.w3.org/ns/odrl/2/lteq"
    const val NEQ = "http://www.w3.org/ns/odrl/2/neq"
    const val HASPART = "http://www.w3.org/ns/odrl/2/hasPart"
    const val ISPARTOF = "http://www.w3.org/ns/odrl/2/isPartOf"
    const val ISA = "http://www.w3.org/ns/odrl/2/isA"
    const val ISALLOF = "http://www.w3.org/ns/odrl/2/isAllOf"
    const val ISANYOF = "http://www.w3.org/ns/odrl/2/isAnyOf"
    const val ISNONEOF = "http://www.w3.org/ns/odrl/2/isNoneOf"

    // 3.18) Policy Conflict Strategy
    const val CONFLICT = "http://www.w3.org/ns/odrl/2/conflict"
    const val PERM = "http://www.w3.org/ns/odrl/2/perm"
    const val PROHIBIT = "http://www.w3.org/ns/odrl/2/prohibit"
    const val INVALID = "http://www.w3.org/ns/odrl/2/invalid"

    // 4.5) Constraint Left Operands
    const val DATETIME = "http://www.w3.org/ns/odrl/2/dateTime"

}