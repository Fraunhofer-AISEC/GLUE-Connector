package de.fraunhofer.aisec.glue.tables

import org.ktorm.entity.Entity

interface Lookup : Entity<Lookup> {
    companion object : Entity.Factory<Lookup>()

    val target: String
    val action: String
    val contract: Contract
}
