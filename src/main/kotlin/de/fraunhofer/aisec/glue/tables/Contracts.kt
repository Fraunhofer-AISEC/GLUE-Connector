package de.fraunhofer.aisec.glue.tables

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.iosb.odrl.OdrlPolicy
import org.ktorm.jackson.KtormModule
import org.ktorm.jackson.json
import org.ktorm.schema.Table
import org.ktorm.schema.varchar

class Contracts(objectMapper: ObjectMapper) : Table<Contract>("contracts") {
    val id = varchar("id").primaryKey().bindTo { it.id }
    val contract = json<OdrlPolicy>("contract", objectMapper.copy().registerModules(KtormModule()))
        .bindTo { it.contract }
}