package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BittensorSubmissionExecutor(
    private val extrinsicService: ExtrinsicService
) {

    private val mutex = Mutex()

    suspend fun execute(metaId: Long, chain: Chain, formExtrinsic: FormExtrinsicWithOrigin): BittensorOperationResult = mutex.withLock {
        try {
            val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)
            val execution = extrinsicService.submitExtrinsicAndAwaitExecution(
                chain,
                TransactionOrigin.WalletWithId(metaId),
                options,
                formExtrinsic
            )
                .getOrThrow()

            when (val outcome = execution.outcome) {
                is ExtrinsicDispatch.Ok -> BittensorOperationResult.Success(execution.extrinsicHash)
                is ExtrinsicDispatch.Failed -> BittensorOperationResult.Failure(outcome.error, execution.extrinsicHash)
                is ExtrinsicDispatch.Unknown -> BittensorOperationResult.Unknown(execution.extrinsicHash)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BittensorOperationResult.Failure(e)
        }
    }
}
