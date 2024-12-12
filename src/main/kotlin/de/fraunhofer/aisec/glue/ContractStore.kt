package de.fraunhofer.aisec.glue

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.module.kotlin.readValue
import de.fraunhofer.aisec.glue.negotiation.NegotiationManager
import de.fraunhofer.iosb.odrl.OdrlPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class ContractStore(
    private val negotiationManager: NegotiationManager,
    mapper: ObjectMapper,
) : Closeable {
    private val objectMapper = mapper.copy().apply {
        // Since the ODRL types use generics, we need to preserve types where needed
        activateDefaultTyping(
            polymorphicTypeValidator,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        )
    }

    // Currently this is just backed by an in-memory DB
    private val connection = DriverManager.getConnection("jdbc:duckdb:")
    private val connectionMutex = Mutex()

    // We use prepared statements more or less only to have an easy guard against SQL injections ;)
    // The reduced SQL parsing overhead is a nice but unintentional side effect.
    private val addContractStmt: PreparedStatement
    private val addLookupStmt: PreparedStatement
    private val getContractStmt: PreparedStatement
    private val getLookupStmt: PreparedStatement
    private val getAllContractsStmt: PreparedStatement

    init {
        // TODO: Invert this so the negotiation manager saves contracts
        //  This also allows for better testing and getting policies from other sources
        CoroutineScope(Dispatchers.Default).launch {
            negotiationManager.negotiatedContracts.collect(::addContract)
        }

        // TODO: Once we use a real store, this part should be in some migrations
        val initStmt = connection.createStatement()
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

        // Since we use multiple prepared statements, we have to handle transactions manually
        connection.autoCommit = false

        addContractStmt =
            connection.prepareStatement(
                """
                INSERT INTO contracts
                VALUES ($1, $2)
                """.trimIndent(),
            )
        addLookupStmt =
            connection.prepareStatement(
                """
                INSERT INTO lookup
                VALUES ($1, $2, $3)
                """.trimIndent(),
            )
        getContractStmt =
            connection.prepareStatement(
                """
                SELECT contract
                FROM contracts
                WHERE id = $1
                """.trimIndent(),
            )
        getLookupStmt =
            connection.prepareStatement(
                """
                SELECT c.contract
                FROM contracts c, lookup l
                WHERE c.id = l.contract AND l.target = $1 AND ($2 IS NULL OR l.action = $2)
                """.trimIndent(),
            )
        getAllContractsStmt =
            connection.prepareStatement(
                """
                SELECT contract FROM contracts
                """.trimIndent(),
            )
    }

    suspend fun addContract(contract: OdrlPolicy): Unit =
        transaction {
            val serialized = objectMapper.writeValueAsString(contract)

            addContractStmt.setString(1, contract.uid.toString())
            addContractStmt.setString(2, serialized)
            addContractStmt.execute()

            for (p in contract.permission) {
                // TODO: What do we do if there is no target?
                addLookupStmt.setString(1, p.target?.uid?.toString() ?: continue)
                addLookupStmt.setString(2, p.action.uid.toString())
                addLookupStmt.setString(3, contract.uid.toString())
                addLookupStmt.addBatch()
            }
            addLookupStmt.executeBatch()
        }

    // Quick and dirty adapter to make working with the JDBC resultSet nicer
    private fun ResultSet.rows(): Sequence<ResultSet> =
        sequence {
            while (next()) {
                yield(this@rows)
            }
        }

    suspend fun getContract(id: String): OdrlPolicy? =
        transaction {
            getContractStmt.setString(1, id)
            getContractStmt.executeQuery().use { result ->
                result.rows()
                    .map { r -> r.getString("contract") }
                    .map { c -> objectMapper.readValue<OdrlPolicy>(c) }
                    // Note: Due to the primary key constraint, there can be at most one item here
                    .firstOrNull()
            }
        }

    suspend fun getContract(
        target: String,
        action: String?,
    ): List<OdrlPolicy> =
        transaction {
            getLookupStmt.setString(1, target)
            getLookupStmt.setString(2, action)
            getLookupStmt.executeQuery().use { result ->
                result.rows()
                    .map { r -> r.getString("contract") }
                    .map{ c -> objectMapper.readValue<OdrlPolicy>(c) }
                    // We have to do eager evaluation here since we want to close the result set within this function
                    .toList()
            }
        }

    suspend fun contracts(): List<OdrlPolicy> =
        transaction {
            getAllContractsStmt.executeQuery().use { result ->
                result.rows()
                    .map { r -> r.getString("contract") }
                    .map { c -> objectMapper.readValue<OdrlPolicy>(c) }
                    .toList()
            }
        }

    override fun close() {
        try {
            addContractStmt.close()
            addLookupStmt.close()
            getContractStmt.close()
            getLookupStmt.close()
            getAllContractsStmt.close()
            connection.close()
        } catch (e: SQLException) {
            LOG.error("Could not close the contract store properly", e)
        }
    }

    private suspend fun <T> transaction(action: suspend () -> T): T =
        connectionMutex.withLock {
            return withContext(Dispatchers.IO) {
                try {
                    val result = action()
                    connection.commit()
                    result
                } catch (e: Throwable) {
                    connection.rollback()
                    throw e
                }
            }
        }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractStore::class.java)
    }
}
