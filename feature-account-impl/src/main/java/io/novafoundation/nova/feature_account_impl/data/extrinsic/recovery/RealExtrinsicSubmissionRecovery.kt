package io.novafoundation.nova.feature_account_impl.data.extrinsic.recovery

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionResolution
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novafoundation.nova.common.utils.extrinsicHash
import java.math.BigInteger

class RealExtrinsicSubmissionRecovery(
    private val pending: ExtrinsicSubmissionJournal,
    private val source: ExtrinsicRecoverySource,
    private val logger: Logger
) : ExtrinsicSubmissionRecovery {

    private val mutex = Mutex()

    override fun pendingHash(metaId: Long): String? = pending.pendingHash(metaId)

    suspend fun recordSigned(metaId: Long, chainId: String, extrinsic: SendableExtrinsic) {
        require(!containsMultisigCall(extrinsic.extrinsic.call)) { "Approval-only transactions require a multisig execution tracker" }
        val anchor = source.finalizedBlock(chainId)
        val expiry = source.expiry(chainId, extrinsic)
        val submission = PendingExtrinsicSubmission(
            extrinsic.extrinsicHex.extrinsicHash(),
            chainId,
            anchor,
            anchor.number + BigInteger.ONE,
            expiry
        )
        pending.record(metaId, submission)
    }

    override fun observe(metaId: Long, chainId: String): Flow<ExtrinsicRecoveryState> = flow {
        while (true) {
            emit(reconcile(metaId, chainId))
            delay(6_000)
        }
    }.distinctUntilChanged()

    override suspend fun reconcile(metaId: Long, chainId: String): ExtrinsicRecoveryState = mutex.withLock {
        val submission = pending.pending(metaId)
            ?: return@withLock pending.resolution(metaId)?.let(ExtrinsicRecoveryState::Resolved)
                ?: ExtrinsicRecoveryState.None
        // A hash alone cannot prove absence. Never silently clear a legacy or different-chain record.
        if (submission.chainId != chainId || submission.anchor == null || submission.nextBlock == null || submission.expiresAt == null) {
            return@withLock ExtrinsicRecoveryState.Pending(submission.hash)
        }
        try {
            withTimeout(25_000) {
                val finalized = source.finalizedBlock(chainId)
                check(finalized.number >= submission.anchor.number) { "Finalized head is behind submission anchor" }
                check(source.blockHash(chainId, submission.anchor.number) == submission.anchor.hash) { "Finalized anchor changed" }
                val end = finalized.number.min(submission.expiresAt - BigInteger.ONE)
                var next: BigInteger = submission.nextBlock
                repeat(64) {
                    if (next > end) return@repeat
                    val hash = source.blockHash(chainId, next)
                    val outcome = source.outcome(chainId, hash, submission.hash)
                    if (outcome != null) {
                        check(outcome != RecoveredExtrinsicOutcome.EXPIRED)
                        val result = ExtrinsicSubmissionResolution(submission.hash, outcome)
                        pending.resolved(metaId, result)
                        return@withTimeout ExtrinsicRecoveryState.Resolved(result)
                    }
                    next += BigInteger.ONE
                    pending.advance(metaId, submission.hash, next)
                }
                if (finalized.number >= submission.expiresAt && next >= submission.expiresAt) {
                    val result = ExtrinsicSubmissionResolution(submission.hash, RecoveredExtrinsicOutcome.EXPIRED)
                    pending.resolved(metaId, result)
                    ExtrinsicRecoveryState.Resolved(result)
                } else {
                    ExtrinsicRecoveryState.Pending(submission.hash)
                }
            }
        } catch (e: CancellationException) {
            if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            reportFailure(e)
            ExtrinsicRecoveryState.Pending(submission.hash)
        } catch (e: Exception) {
            reportFailure(e)
            ExtrinsicRecoveryState.Pending(submission.hash)
        }
    }

    override suspend fun acknowledge(metaId: Long, hash: String) = pending.acknowledge(metaId, hash)

    private fun reportFailure(cause: Throwable) {
        logger.log("[ExtrinsicRecovery] Unable to verify previous submission; keeping it blocked")
        logger.log(cause)
    }
}
