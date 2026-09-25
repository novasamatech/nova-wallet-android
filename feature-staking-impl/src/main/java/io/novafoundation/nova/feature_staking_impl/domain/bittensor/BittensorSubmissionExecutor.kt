package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPendingSubmissionRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A lost response is not proof of rejection. Pending hashes are never cleared by an error or timeout. */
class BittensorSubmissionExecutor(
    private val extrinsics: ExtrinsicService,
    private val pending: BittensorPendingSubmissionRepository
) {
    private val mutex = Mutex()

    suspend fun execute(metaId: Long, chain: Chain, formExtrinsic: FormExtrinsicWithOrigin): BittensorOperationResult = mutex.withLock {
        pending.pendingHash(metaId)?.let { return@withLock BittensorOperationResult.Unknown(it) }
        var signedHash: String? = null
        val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL, onSigned = { hash ->
            pending.record(metaId, hash)
            signedHash = hash
        })
        try {
            val execution = extrinsics.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.WalletWithId(metaId), options, formExtrinsic)
                .getOrThrow()
            when (val outcome = execution.outcome) {
                is ExtrinsicDispatch.Ok -> {
                    pending.resolved(metaId, execution.extrinsicHash)
                    BittensorOperationResult.Success(execution.extrinsicHash)
                }
                is ExtrinsicDispatch.Failed -> {
                    pending.resolved(metaId, execution.extrinsicHash)
                    BittensorOperationResult.Failure(outcome.error)
                }
                ExtrinsicDispatch.Unknown -> BittensorOperationResult.Unknown(execution.extrinsicHash)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            signedHash?.let { BittensorOperationResult.Unknown(it) } ?: BittensorOperationResult.Failure(e)
        }
    }
}
