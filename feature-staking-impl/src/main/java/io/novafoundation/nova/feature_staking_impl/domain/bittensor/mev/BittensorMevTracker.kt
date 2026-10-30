package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevProtocol
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExtrinsicOutcomeResolver
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface BittensorMevExecution {

    class Executed(
        val inner: ExtrinsicExecutionResult,
        val carrier: ExtrinsicExecutionResult?,
        val carrierState: BittensorMevCarrier
    ) : BittensorMevExecution

    class NotExecuted(
        val carrier: BittensorMevCarrier,
        val carrierHash: String,
        val carrierExecution: ExtrinsicExecutionResult? = null
    ) : BittensorMevExecution

    class Unconfirmed(val innerHash: String) : BittensorMevExecution
}

class BittensorMevTracker(
    private val rpc: RpcCalls,
    private val outcomeResolver: BittensorExtrinsicOutcomeResolver,
    private val logger: Logger
) {

    suspend fun submitAndTrack(
        chain: Chain,
        timing: BittensorMevTiming,
        inner: SendableExtrinsic,
        carrier: SendableExtrinsic,
        hierarchy: SubmissionHierarchy,
        blockMillis: Long,
        onUnconfirmed: (String) -> Unit = {}
    ): BittensorMevExecution = coroutineScope {
        val innerHash = inner.extrinsicHex.extrinsicHash()
        val carrierHash = carrier.extrinsicHex.extrinsicHash()
        val watch = launch {
            try {
                rpc.submitAndWatchExtrinsic(chain.id, carrier).first { it.terminal || it is ExtrinsicStatus.InBlock }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A reconnect may have broadcast an earlier copy. Only the finalized scan proves non-execution.
                logger.log("[BittensorMev] Carrier watch lost; resolving finalized era")
                logger.log(e)
            }
        }
        try {
            val resolution = async { scan(chain, timing, innerHash, carrierHash, hierarchy, blockMillis) }
            val confirmationTimeout = maxOf(
                BittensorMevProtocol.MIN_CONFIRMATION_TIMEOUT_MILLIS,
                blockMillis * BittensorMevProtocol.CONFIRMATION_WINDOW_BLOCKS
            )
            withTimeoutOrNull(confirmationTimeout) { resolution.await() } ?: run {
                onUnconfirmed(innerHash)
                // The screen can close now; while it remains open, a late finalized result replaces uncertainty.
                resolution.await()
            }
        } finally {
            watch.cancel()
        }
    }

    private suspend fun scan(
        chain: Chain,
        timing: BittensorMevTiming,
        innerHash: String,
        carrierHash: String,
        hierarchy: SubmissionHierarchy,
        blockMillis: Long
    ): BittensorMevExecution {
        var height = timing.birth
        val lastHeight = timing.birth + BittensorMevProtocol.ERA_BLOCKS - 1
        var pollsLeft = BittensorMevProtocol.SCAN_POLLS
        var carrierState = BittensorMevCarrier.ABSENT
        var carrierExecution: ExtrinsicExecutionResult? = null
        while (pollsLeft > 0) {
            var found: BittensorMevExecution.Executed? = null
            val inspected = try {
                withTimeoutOrNull(blockMillis * BittensorMevProtocol.READ_TIMEOUT_BLOCKS) {
                    val finalizedHash = rpc.getFinalizedHead(chain.id)
                    val finalized = rpc.getBlockHeader(chain.id, finalizedHash).number
                    if (finalized < height) return@withTimeoutOrNull false
                    val blockHash = rpc.getBlockHash(chain.id, height.toBigInteger())
                    val hashes = rpc.getBlock(chain.id, blockHash).block.extrinsics.map { it.extrinsicHash() }
                    if (carrierHash in hashes && carrierState == BittensorMevCarrier.ABSENT) {
                        val execution = resolve(chain, carrierHash, blockHash, hierarchy)
                        carrierExecution = execution.takeUnless { it.outcome == ExtrinsicDispatch.Unknown }
                        carrierState = if (carrierExecution == null) BittensorMevCarrier.UNKNOWN else BittensorMevCarrier.INCLUDED
                    }
                    if (innerHash in hashes) {
                        val execution = resolve(chain, innerHash, blockHash, hierarchy)
                        if (execution.outcome == ExtrinsicDispatch.Unknown) return@withTimeoutOrNull false
                        found = BittensorMevExecution.Executed(execution, carrierExecution, carrierState)
                    }
                    true
                } ?: false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorMev] Finalized block lookup failed; retrying the same height")
                logger.log(e)
                false
            }
            found?.let { return it }
            if (inspected) {
                if (height == lastHeight) return BittensorMevExecution.NotExecuted(carrierState, carrierHash, carrierExecution)
                height += 1
            } else {
                pollsLeft -= 1
                if (pollsLeft > 0) delay((blockMillis / 2).coerceAtLeast(1))
            }
        }
        return BittensorMevExecution.Unconfirmed(innerHash)
    }

    private suspend fun resolve(chain: Chain, hash: String, blockHash: String, hierarchy: SubmissionHierarchy): ExtrinsicExecutionResult {
        val status = ExtrinsicStatus.InBlock(blockHash, hash)
        return outcomeResolver.resolve(chain, ExtrinsicWatchResult(status, hierarchy))
    }
}
