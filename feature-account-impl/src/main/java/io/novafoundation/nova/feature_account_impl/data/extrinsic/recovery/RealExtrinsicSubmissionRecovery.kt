package io.novafoundation.nova.feature_account_impl.data.extrinsic.recovery

import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionResolution
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class RealExtrinsicSubmissionRecovery(
    private val submissionJournal: ExtrinsicSubmissionJournal,
    private val recoverySource: ExtrinsicRecoverySource,
    private val logger: Logger
) : ExtrinsicSubmissionRecovery {

    private val mutex = Mutex()

    override fun pendingHash(metaId: Long): String? = submissionJournal.pendingHash(metaId)

    suspend fun recordSigned(
        metaId: Long,
        chainId: String,
        extrinsic: SendableExtrinsic
    ) {
        require(!containsMultisigCall(extrinsic.extrinsic.call)) { "Approval-only transactions require a multisig execution tracker" }
        val anchor = recoverySource.finalizedBlock(chainId)
        val expiry = recoverySource.expiry(chainId, extrinsic)
        val pendingSubmission = PendingExtrinsicSubmission(
            extrinsic.extrinsicHex.extrinsicHash(),
            chainId,
            anchor,
            anchor.number + BigInteger.ONE,
            expiry
        )
        submissionJournal.record(metaId, pendingSubmission)
    }

    override fun observe(metaId: Long, chainId: String): Flow<ExtrinsicRecoveryState> = flow {
        while (true) {
            emit(reconcile(metaId, chainId))
            delay(POLL_INTERVAL_MILLIS)
        }
    }
        .distinctUntilChanged()

    override suspend fun reconcile(metaId: Long, chainId: String): ExtrinsicRecoveryState = mutex.withLock {
        val pendingSubmission = submissionJournal.pending(metaId)
            ?: return@withLock submissionJournal.resolution(metaId)
                ?.let(ExtrinsicRecoveryState::Resolved)
                ?: ExtrinsicRecoveryState.None
        // A hash alone cannot prove absence. Never silently clear a legacy or different-chain record.
        if (
            pendingSubmission.chainId != chainId ||
            pendingSubmission.anchor == null ||
            pendingSubmission.nextBlock == null ||
            pendingSubmission.expiresAt == null
        ) {
            return@withLock ExtrinsicRecoveryState.Pending(pendingSubmission.hash)
        }
        try {
            withTimeout(RECONCILIATION_TIMEOUT_MILLIS) {
                scanFinalizedBlocks(metaId, chainId, pendingSubmission)
            }
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            reportFailure(e)
            ExtrinsicRecoveryState.Pending(pendingSubmission.hash)
        } catch (e: Exception) {
            reportFailure(e)
            ExtrinsicRecoveryState.Pending(pendingSubmission.hash)
        }
    }

    override suspend fun acknowledge(metaId: Long, hash: String) = submissionJournal.acknowledge(metaId, hash)

    private suspend fun scanFinalizedBlocks(
        metaId: Long,
        chainId: String,
        pendingSubmission: PendingExtrinsicSubmission
    ): ExtrinsicRecoveryState {
        val anchor = requireNotNull(pendingSubmission.anchor)
        val expiresAt = requireNotNull(pendingSubmission.expiresAt)
        val finalized = recoverySource.finalizedBlock(chainId)
        check(finalized.number >= anchor.number) { "Finalized head is behind submission anchor" }
        check(recoverySource.blockHash(chainId, anchor.number) == anchor.hash) { "Finalized anchor changed" }
        val end = finalized.number.min(expiresAt - BigInteger.ONE)
        var nextBlock = requireNotNull(pendingSubmission.nextBlock)
        var scannedBlocks = 0

        while (nextBlock <= end && scannedBlocks < MAX_BLOCKS_PER_SCAN) {
            val blockHash = recoverySource.blockHash(chainId, nextBlock)
            val outcome = recoverySource.outcome(chainId, blockHash, pendingSubmission.hash)
            if (outcome != null) {
                check(outcome != RecoveredExtrinsicOutcome.EXPIRED)
                return resolve(metaId, pendingSubmission.hash, outcome)
            }

            nextBlock += BigInteger.ONE
            scannedBlocks += 1
            submissionJournal.advance(metaId, pendingSubmission.hash, nextBlock)
        }

        return if (finalized.number >= expiresAt && nextBlock >= expiresAt) {
            resolve(metaId, pendingSubmission.hash, RecoveredExtrinsicOutcome.EXPIRED)
        } else {
            ExtrinsicRecoveryState.Pending(pendingSubmission.hash)
        }
    }

    private suspend fun resolve(metaId: Long, hash: String, outcome: RecoveredExtrinsicOutcome): ExtrinsicRecoveryState.Resolved {
        val resolution = ExtrinsicSubmissionResolution(hash, outcome)
        submissionJournal.resolved(metaId, resolution)
        return ExtrinsicRecoveryState.Resolved(resolution)
    }

    private fun reportFailure(cause: Throwable) {
        logger.log("[ExtrinsicRecovery] Unable to verify previous submission; keeping it blocked")
        logger.log(cause)
    }
    private companion object {

        const val POLL_INTERVAL_MILLIS = 6_000L

        const val RECONCILIATION_TIMEOUT_MILLIS = 25_000L

        const val MAX_BLOCKS_PER_SCAN = 64
    }
}
