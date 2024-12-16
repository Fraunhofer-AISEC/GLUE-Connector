package de.fraunhofer.aisec.glue.tables

import org.ktorm.schema.Table
import org.ktorm.schema.varchar

class Lookups(contractsTable: Contracts) : Table<Lookup>("lookup") {
    val target = varchar("target").primaryKey().bindTo { it.target }
    val action = varchar("action").primaryKey().bindTo { it.action }
    val contract = varchar("contract").primaryKey().references(contractsTable) { it.contract }
}