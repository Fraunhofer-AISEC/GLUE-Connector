package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.tables.Contract
import de.fraunhofer.aisec.glue.tables.Contracts
import de.fraunhofer.aisec.glue.tables.Lookups
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.iosb.odrl.OdrlPolicy
import kotlinx.coroutines.*
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import org.ktorm.dsl.batchInsert
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import org.slf4j.LoggerFactory

class ContractStore(
    private val negotiationManager: NegotiationManager,
    objectMapper: ObjectMapper,
) {
    // Currently this is just backed by an in-memory DB
    private val dataSource by lazy {
        BasicDataSource().apply {
            driverClassName = "org.duckdb.DuckDBDriver"
            url = "jdbc:duckdb:"
        }
    }

    private val connection: Database
        get() = Database.connect(dataSource)

    val contractsTable = Contracts(objectMapper)
    val Database.contracts get() = this.sequenceOf(contractsTable)
    val lookupsTable = Lookups(contractsTable)
    val Database.lookups get() = this.sequenceOf(lookupsTable)

    init {
        //  This also allows for better testing and getting policies from other sources
        CoroutineScope(Dispatchers.Default).launch {
            negotiationManager.negotiatedContracts.collect(::addContract)
        }

        dataSource.connection.use {
            val initStmt = it.createStatement()
            initStmt.addBatch(
                """
                CREATE TABLE contracts(
                    id VARCHAR PRIMARY KEY,
                    contract JSON,
                )
                """.trimIndent(),
            )
            initStmt.addBatch(
                """
                CREATE TABLE lookup(
                    target VARCHAR,
                    action VARCHAR,
                    contract VARCHAR,
                    PRIMARY KEY (target, action, contract),
                    FOREIGN KEY (contract) REFERENCES contracts(id),
                )
                """.trimIndent(),
            )
            initStmt.executeBatch()
        }
    }

    suspend fun addContract(contract: OdrlPolicy): Unit =
        ioAsync {
            connection.useTransaction {
                connection.contracts.add(Contract {
                    this.id = contract.uid.toString()
                    this.contract = contract
                })
                connection.batchInsert(lookupsTable) {
                    for (p in contract.permission) {
                        val targetUid = p.target?.uid?.toString() ?: continue
                        item {
                            set(it.target, targetUid)
                            set(it.action, p.action.uid.toString())
                            set(it.contract, contract.uid.toString())
                        }
                    }
                }
            }
        }

    suspend fun getContract(id: String): OdrlPolicy? =
        ioAsync { connection.contracts.find { it.id eq id }?.contract }

    suspend fun getContract(
        target: String,
        action: String,
    ): List<OdrlPolicy> =
        ioAsync {
            connection.lookups
                .filter { it.target eq target }
                .filter { it.action eq action }
                .map { it.contract.contract }
        }

    suspend fun contracts(): List<OdrlPolicy> =
        ioAsync { connection.contracts.map { it.contract } }

    suspend inline fun <T> ioAsync(crossinline block: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.IO) { block() }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractStore::class.java)
    }
}
