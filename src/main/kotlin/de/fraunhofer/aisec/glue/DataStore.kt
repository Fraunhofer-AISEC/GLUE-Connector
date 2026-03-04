package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.negotiation.builtin.BuiltinNegotiationData
import de.fraunhofer.aisec.glue.tables.Contracts
import de.fraunhofer.aisec.glue.tables.Lookups
import de.fraunhofer.aisec.glue.tables.Negotiations
import de.fraunhofer.iosb.odrl.OdrlPolicy
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.commons.dbcp2.BasicDataSource
import org.flywaydb.core.Flyway
import org.ktorm.database.Database
import org.ktorm.dsl.batchInsert
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.isNull
import org.ktorm.dsl.update
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.map
import org.ktorm.entity.sequenceOf
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor
import kotlin.io.path.createTempDirectory

class DataStore(
    objectMapper: ObjectMapper,
) {
    // Currently, this is just backed by an in-memory DB
    private val dataSource by lazy {
        // Make sure we end up with a unique db every time.
        // Note: On linux, the TMP dir is usually cleaned on reboot. On windows, this could lead to a bunch of junk
        // files over time.
        val path = createTempDirectory().resolve("contracts.db")
        BasicDataSource().apply {
            driverClassName = "org.duckdb.DuckDBDriver"
            url = "jdbc:duckdb:$path"
        }
    }

    // Let Flyway handle the database setup/migrations
    init {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }

    private val connection: Database
        get() = Database.connect(dataSource)

    val contractsTable = Contracts(objectMapper)
    val Database.contracts get() = this.sequenceOf(contractsTable)
    val lookupsTable = Lookups(contractsTable)
    val Database.lookups get() = this.sequenceOf(lookupsTable)

    val negotiationsTable = Negotiations(objectMapper)
    val Database.negotiations get() = this.sequenceOf(negotiationsTable)

    suspend fun addContract(contract: OdrlPolicy): Unit =
        ioAsync {
            connection.useTransaction {
                connection.insert(contractsTable) {
                    set(it.id, contract.uid.toString())
                    set(it.contract, contract)
                }
                connection.batchInsert(lookupsTable) {
                    for (p in contract.permission) {
                        val targetUid = p.target?.uid?.toString() ?: continue
                        item {
                            set(it.target, targetUid)
                            set(it.action, p.action.value.uid.toString())
                            set(it.contract, contract.uid.toString())
                        }
                    }
                }
            }
        }

    suspend fun getContract(id: String): OdrlPolicy? = ioAsync { connection.contracts.find { it.id eq id }?.contract }

    suspend fun getContract(
        target: String,
        action: String? = null,
    ): List<OdrlPolicy> =
        ioAsync {
            connection.lookups
                .filter { it.target eq target }
                .filter { action?.let { a -> it.action eq a } ?: it.action.isNull() }
                .map { it.contract.contract }
        }

    suspend fun contracts(): List<OdrlPolicy> = ioAsync { connection.contracts.map { it.contract } }

    suspend fun addNegotiation(data: BuiltinNegotiationData) =
        ioAsync {
            connection.insert(negotiationsTable) {
                set(it.id, data.id)
                set(it.role, data.role)
                set(it.peer, data.peer)
                set(it.peerPid, data.peerPid)
                set(it.startTimestamp, data.startTimestamp)
                set(it.state, data.state)
                set(it.callbackUrl, data.callbackUrl.toString())
                set(it.peerUrl, data.peerUrl.toString())
                set(it.contract, data.contract)
                set(it.failureReason, data.failureReason)
            }
        }

    suspend fun negotiations(): List<BuiltinNegotiationData> =
        ioAsync {
            connection.negotiations.map {
                BuiltinNegotiationData(
                    it.id,
                    it.role,
                    it.peer,
                    it.peerPid,
                    it.startTimestamp,
                    it.state,
                    Url(it.callbackUrl),
                    Url(it.peerUrl),
                    it.contract,
                    it.failureReason,
                )
            }
        }

    suspend fun updateNegotiation(data: BuiltinNegotiationData) =
        ioAsync {
            connection.update(negotiationsTable) {
                where { it.id eq data.id }

                set(it.role, data.role)
                set(it.peer, data.peer)
                set(it.peerPid, data.peerPid)
                set(it.startTimestamp, data.startTimestamp)
                set(it.state, data.state)
                set(it.callbackUrl, data.callbackUrl.toString())
                set(it.peerUrl, data.peerUrl.toString())
                set(it.contract, data.contract)
                set(it.failureReason, data.failureReason)
            }
        }

    suspend fun deleteNegotiation(data: BuiltinNegotiationData) =
        ioAsync {
            connection.delete(negotiationsTable) {
                it.id eq data.id
            }
        }

    suspend inline fun <T> ioAsync(crossinline block: suspend CoroutineScope.() -> T): T = withContext(LOOM_DISPATCHER) { block() }

    companion object {
        // see https://kt.academy/article/dispatcher-for-backend#why-loom-dispatcher-is-a-perfect-choice--but-only-once-it-gets-stable
        val LOOM_DISPATCHER = newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
        private val LOG = LoggerFactory.getLogger(DataStore::class.java)
    }
}
