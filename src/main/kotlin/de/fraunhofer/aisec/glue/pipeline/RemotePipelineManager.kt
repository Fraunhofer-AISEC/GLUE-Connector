package de.fraunhofer.aisec.glue.pipeline

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.glue.Utils.hexEncode
import de.fraunhofer.aisec.glue.Utils.makeHttpException
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateRequest
import de.fraunhofer.aisec.glue.models.pipeline.PipelineCreateResponse
import de.fraunhofer.iosb.odrl.OdrlIdentifiers
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class RemotePipelineManager(val httpClient: HttpClient, val objectMapper: ObjectMapper) {
    // We need a mutex as we have more than one lookup table
    private val pipelinesMutex = Mutex()

    // TODO: Persist
    private val remotePipelines = mutableMapOf<String, RemotePipeline>()
    private val lookupByContract = mutableMapOf<Triple<String, String, String>, RemotePipeline>()

    // TODO: Check if we can make some parameters nullable here?
    // TODO: Maybe do finer grained logging so we can have at least some parallelism
    private suspend fun createUnlocked(
        peerUrl: Url,
        contractId: String,
        targetId: String,
        actionId: String,
    ): RemotePipeline {
        if (lookupByContract.contains(Triple(contractId, targetId, actionId))) {
            throw IllegalStateException("Pipeline for this contract, target and action already exists")
        }

        val createReq = PipelineCreateRequest(targetId, contractId, actionId)
        val createResp =
            httpClient.post(URLBuilder(peerUrl).appendPathSegments("pipeline").build()) {
                contentType(ContentType.Application.Json)
                setBody(createReq)
            }

        if (!createResp.status.isSuccess()) {
            val err = makeHttpException(createResp)
            LOG.atError {
                message = "Got error for remote pipeline creation request"
                cause = err
                payload = mapOf("peerUrl" to peerUrl)
            }
            throw err
        }

        val pipelineInfo = createResp.body<PipelineCreateResponse>()
        // We prefer to use the same ID remote and locally, but that may not always be possible
        val pipelineId =
            if (remotePipelines.contains(pipelineInfo.pipelineName)) {
                generateSequence { Random.nextBytes(8).hexEncode() }.first { n -> !remotePipelines.contains(n) }
            } else {
                pipelineInfo.pipelineName
            }

        LOG.atDebug {
            message = "Successfully created remote pipeline"
            payload =
                mapOf(
                    "id" to pipelineInfo.pipelineName,
                    "localId" to pipelineId,
                    "peer" to peerUrl,
                )
        }

        val pipeline =
            RemotePipeline(
                pipelineId,
                pipelineInfo.pipelineName,
                peerUrl,
                pipelineInfo.ports,
                contractId,
                targetId,
                actionId,
                httpClient,
            )

        remotePipelines[pipelineId] = pipeline
        lookupByContract[Triple(contractId, targetId, actionId)] = pipeline

        return pipeline
    }

    /**
     * Create a new remote pipeline for the given contract, target and action combo.
     * This function throws when a suitable pipeline already exists.
     */
    suspend fun create(
        peerUrl: Url,
        contractId: String,
        targetId: String,
        actionId: String = OdrlIdentifiers.USE,
    ): RemotePipeline = pipelinesMutex.withLock { createUnlocked(peerUrl, contractId, targetId, actionId) }

    fun getAll(): Collection<RemotePipeline> {
        return remotePipelines.values
    }

    suspend fun getById(pipelineId: String): RemotePipeline? =
        pipelinesMutex.withLock {
            remotePipelines[pipelineId]
        }

    suspend fun getByContract(
        contractId: String,
        targetId: String,
        actionId: String = OdrlIdentifiers.USE,
    ): RemotePipeline? =
        pipelinesMutex.withLock {
            lookupByContract[Triple(contractId, targetId, actionId)]
        }

    suspend fun getOrCreate(
        peerUrl: Url,
        contractId: String,
        targetId: String,
        actionId: String = OdrlIdentifiers.USE,
    ): RemotePipeline =
        pipelinesMutex.withLock {
            val candidate = lookupByContract[Triple(contractId, targetId, actionId)]
            // If the remote pipeline is still up, we assume we can reuse it
            if (candidate?.status() == true) {
                LOG.atDebug {
                    message = "Returning preexisting pipeline to caller as it is still up"
                    payload = mapOf("id" to candidate.id)
                }
                return@withLock candidate
            }
            candidate?.also {
                LOG.atDebug {
                    message = "Removing old remote pipeline as it is dead"
                    payload = mapOf("id" to candidate.id)
                }
                // TODO: Also somehow remove this from pipelines
                //  Some form of universal event system would be nice here, but we don't want to fall into the "clean code" trap either...
                //  Instead, we should probably male the pipelines automatically replace remote pipelines based on the target contract
                //  somehow.
                remotePipelines.remove(candidate.id)
                lookupByContract.remove(Triple(candidate.contractId, candidate.targetId, candidate.actionId))
            }
            createUnlocked(peerUrl, contractId, targetId, actionId)
        }

    suspend fun delete(pipelineId: String): Boolean =
        pipelinesMutex.withLock {
            val pipeline = remotePipelines.remove(pipelineId) ?: return@withLock false
            lookupByContract.remove(Triple(pipeline.contractId, pipeline.targetId, pipeline.actionId))
            pipeline.stop()
            true
        }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}
