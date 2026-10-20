/*
 * Copyright 2024 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.odrl

object OdrlIdentifiers {
    // ODRL vocabulary: https://www.w3.org/TR/odrl-vocab
    const val CONTEXT = "http://www.w3.org/ns/odrl.jsonld"

    // 3) ODRL Core Vocabulary

    // 3.1) Policy
    const val POLICY = "http://www.w3.org/ns/odrl/2/Policy"
    const val UID = "http://www.w3.org/ns/odrl/2/uid"
    const val PROFILE = "http://www.w3.org/ns/odrl/2/profile"
    const val INHERIT_FROM = "http://www.w3.org/ns/odrl/2/inheritFrom"

    // 3.2) Policy subclasses
    const val AGREEMENT = "http://www.w3.org/ns/odrl/2/Agreement"
    const val OFFER = "http://www.w3.org/ns/odrl/2/Offer"
    const val SET = "http://www.w3.org/ns/odrl/2/Set"

    // 3.3) Rule
    const val RULE = "http://www.w3.org/ns/odrl/2/Rule"
    const val RELATION = "http://www.w3.org/ns/odrl/2/relation"
    const val FUNCTION = "http://www.w3.org/ns/odrl/2/function"
    const val FAILURE = "http://www.w3.org/ns/odrl/2/failure"

    // 3.4) Asset
    const val ASSET = "http://www.w3.org/ns/odrl/2/Asset"
    const val ASSET_COLLECTION = "http://www.w3.org/ns/odrl/2/AssetCollection"

    // 3.5) Asset Relations
    const val TARGET = "http://www.w3.org/ns/odrl/2/target"
    const val TARGET_POLICY = "http://www.w3.org/ns/odrl/2/hasPolicy"

    // 3.6) Party
    const val PARTY = "http://www.w3.org/ns/odrl/2/Party"
    const val PARTY_COLLECTION = "http://www.w3.org/ns/odrl/2/PartyCollection"

    // 3.7) Party Functions
    const val ASSIGNEE = "http://www.w3.org/ns/odrl/2/assignee"
    const val ASSIGNER = "http://www.w3.org/ns/odrl/2/assigner"
    const val ASSIGNEE_OF = "http://www.w3.org/ns/odrl/2/assigneeOf"
    const val ASSIGNER_OF = "http://www.w3.org/ns/odrl/2/assignerOf"

    // 3.8) Asset and Party
    const val PART_OF = "http://www.w3.org/ns/odrl/2/partOf"
    const val SOURCE = "http://www.w3.org/ns/odrl/2/source"

    // 3.9) Permission
    const val PERMISSION = "http://www.w3.org/ns/odrl/2/Permission"
    const val HAS_PERMISSION = "http://www.w3.org/ns/odrl/2/permission"

    // 3.10) Prohibition
    const val PROHIBITION = "http://www.w3.org/ns/odrl/2/Prohibition"
    const val HAS_PROHIBITION = "http://www.w3.org/ns/odrl/2/prohibition"

    // 3.11) Action
    const val ACTION = "http://www.w3.org/ns/odrl/2/Action"
    const val HAS_ACTION = "http://www.w3.org/ns/odrl/2/action"
    const val INCLUDED_IN = "http://www.w3.org/ns/odrl/2/includedIn"
    const val IMPLIES = "http://www.w3.org/ns/odrl/2/implies"

    // 3.12) Actions for Rules
    const val USE = "http://www.w3.org/ns/odrl/2/use"
    const val TRANSFER_OWNERSHIP = "http://www.w3.org/ns/odrl/2/transfer"

    // 3.13) Duty
    const val DUTY = "http://www.w3.org/ns/odrl/2/Duty"
    const val OBLIGATION = "http://www.w3.org/ns/odrl/2/obligation"
    const val HAS_DUTY = "http://www.w3.org/ns/odrl/2/duty"
    const val CONSEQUENCE = "http://www.w3.org/ns/odrl/2/consequence"
    const val REMEDY = "http://www.w3.org/ns/odrl/2/remedy"

    // 3.14) Constraint
    const val CONSTRAINT = "http://www.w3.org/ns/odrl/2/Constraint"
    const val HAS_CONSTRAINT = "http://www.w3.org/ns/odrl/2/constraint"
    const val REFINEMENT = "http://www.w3.org/ns/odrl/2/refinement"
    const val OPERATOR = "http://www.w3.org/ns/odrl/2/Operator"
    const val HAS_OPERATOR = "http://www.w3.org/ns/odrl/2/operator"
    const val RIGHT_OPERAND = "http://www.w3.org/ns/odrl/2/RightOperand"
    const val HAS_RIGHT_OPERAND = "http://www.w3.org/ns/odrl/2/rightOperand"
    const val HAS_RIGHT_OPERAND_REFERENCE = "http://www.w3.org/ns/odrl/2/rightOperandReference"
    const val LEFT_OPERAND = "http://www.w3.org/ns/odrl/2/LeftOperand"
    const val HAS_LEFT_OPERAND = "http://www.w3.org/ns/odrl/2/leftOperand"
    const val UNIT = "http://www.w3.org/ns/odrl/2/unit"
    const val DATATYPE = "http://www.w3.org/ns/odrl/2/dataType"
    const val STATUS = "http://www.w3.org/ns/odrl/2/status"

    // 3.15) Logical Constraint
    const val LOGICAL_CONSTRAINT = "http://www.w3.org/ns/odrl/2/LogicalConstraint"
    const val OPERAND = "http://www.w3.org/ns/odrl/2/operand"

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

    // 3.17) Logical Constraint Operands
    const val OR = "http://www.w3.org/ns/odrl/2/or"
    const val XONE = "http://www.w3.org/ns/odrl/2/xone"
    const val AND = "http://www.w3.org/ns/odrl/2/and"
    const val AND_SEQUENCE = "http://www.w3.org/ns/odrl/2/andSequence"

    // 3.18) Policy Conflict Strategy
    const val CONFLICT_TERM = "http://www.w3.org/ns/odrl/2/ConflictTerm"
    const val CONFLICT = "http://www.w3.org/ns/odrl/2/conflict"
    const val PERM = "http://www.w3.org/ns/odrl/2/perm"
    const val PROHIBIT = "http://www.w3.org/ns/odrl/2/prohibit"
    const val INVALID = "http://www.w3.org/ns/odrl/2/invalid"

    // 4) ODRL Common Vocabulary

    // 4.2) Asset Relations
    const val OUTPUT = "http://www.w3.org/ns/odrl/2/output"

    // 4.3) Party Functions
    const val ATTRIBUTED_PARTY = "http://www.w3.org/ns/odrl/2/attributedParty"
    const val ATTRIBUTING_PARTY = "http://www.w3.org/ns/odrl/2/attributingParty"
    const val COMPENSATED_PARTY = "http://www.w3.org/ns/odrl/2/compensatedParty"
    const val COMPENSATING_PARTY = "http://www.w3.org/ns/odrl/2/compensatingParty"
    const val CONSENTED_PARTY = "http://www.w3.org/ns/odrl/2/consentedParty"
    const val CONSENTING_PARTY = "http://www.w3.org/ns/odrl/2/consentingParty"
    const val CONTRACTED_PARTY = "http://www.w3.org/ns/odrl/2/contractedParty"
    const val CONTRACTING_PARTY = "http://www.w3.org/ns/odrl/2/contractingParty"
    const val INFORMED_PARTY = "http://www.w3.org/ns/odrl/2/informedParty"
    const val INFORMING_PARTY = "http://www.w3.org/ns/odrl/2/informingParty"
    const val TRACKED_PARTY = "http://www.w3.org/ns/odrl/2/trackedParty"
    const val TRACKING_PARTY = "http://www.w3.org/ns/odrl/2/trackingParty"

    // 4.4) Actions for Rules
    const val NEXTPOLICY = "http://www.w3.org/ns/odrl/2/nextPolicy"
    const val DISTRIBUTE = "http://www.w3.org/ns/odrl/2/distribute"

    // 4.5) Constraint Left Operands
    const val DATETIME = "http://www.w3.org/ns/odrl/2/dateTime"
}
