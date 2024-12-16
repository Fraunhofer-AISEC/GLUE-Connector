package de.fraunhofer.aisec.glue.tables

import de.fraunhofer.iosb.odrl.OdrlPolicy
import org.ktorm.entity.Entity

interface Contract : Entity<Contract> {
    companion object : Entity.Factory<Contract>()
    var id: String
    var contract: OdrlPolicy
}