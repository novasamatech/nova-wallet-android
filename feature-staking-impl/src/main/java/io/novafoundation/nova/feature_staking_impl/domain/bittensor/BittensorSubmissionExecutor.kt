package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** A lost response is not proof of rejection. Pending hashes are never cleared by an error or timeout. */
class BittensorSubmissionExecutor(
    private val extrinsics: ExtrinsicService,
    private val recovery: ExtrinsicSubmissionRecovery
) {
    private val mutex = Mutex()

    suspend fun execute(metaId: Long, chain: Chain, formExtrinsic: FormExtrinsicWithOrigin): BittensorOperationResult = mutex.withLock {
        recovery.pendingHash(metaId)?.let { return@withLock BittensorOperationResult.Unknown(it) }
        try {
            val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL, recoverable = true)
            val execution = extrinsics.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.WalletWithId(metaId), options, formExtrinsic)
                .getOrThrow()
            // Inclusion can be reorged. Only finalized evidence releases the wallet's lock.
            val resolution = withTimeoutOrNull(60_000) {
                while (true) {
                    when (val state = recovery.reconcile(metaId, chain.id)) {
                        is ExtrinsicRecoveryState.Resolved -> return@withTimeoutOrNull state.result
                        else -> delay(3_000)
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                null
            }
            when (resolution?.outcome) {
                RecoveredExtrinsicOutcome.SUCCESS -> BittensorOperationResult.Success(execution.extrinsicHash)
                RecoveredExtrinsicOutcome.FAILURE -> BittensorOperationResult.Failure(
                    (execution.outcome as? ExtrinsicDispatch.Failed)?.error ?: IllegalStateException("Transaction failed in a finalized block"),
                    execution.extrinsicHash
                )
                RecoveredExtrinsicOutcome.EXPIRED -> BittensorOperationResult.Failure(
                    IllegalStateException("Transaction expired without inclusion"),
                    execution.extrinsicHash
                )
                null -> BittensorOperationResult.Unknown(execution.extrinsicHash)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recovery.pendingHash(metaId)?.let { BittensorOperationResult.Unknown(it) } ?: BittensorOperationResult.Failure(e)
        }
    }
}
