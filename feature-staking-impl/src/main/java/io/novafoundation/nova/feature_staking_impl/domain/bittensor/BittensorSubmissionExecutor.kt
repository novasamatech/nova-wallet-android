package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_staking_impl.data.bittensor.bindBittensorOperationOutcome
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BittensorSubmissionExecutor(
    private val extrinsicService: ExtrinsicService,
    private val positionUpdates: BittensorPositionUpdates
) {

    private val mutex = Mutex()

    suspend fun execute(
        metaId: Long,
        chain: Chain,
        target: BittensorOutcomeTarget? = null,
        formExtrinsic: FormExtrinsicWithOrigin
    ): BittensorOperationResult = mutex.withLock {
        try {
            val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)
            val execution = extrinsicService.submitExtrinsicAndAwaitExecution(
                chain,
                TransactionOrigin.WalletWithId(metaId),
                options,
                formExtrinsic
            )
                .getOrThrow()

            val result = execution.mapBittensorOperationResult(chain)
            val coldkey = execution.submissionHierarchy.path.lastOrNull()?.account
                ?.takeIf { it.id == metaId }?.accountIdIn(chain)?.let(::AccountIdKey)
            val payer = execution.submissionHierarchy.path.firstOrNull()?.account?.accountIdIn(chain)?.let(::AccountIdKey)
            val events = when (val dispatch = execution.outcome) {
                is ExtrinsicDispatch.Ok -> dispatch.emittedEvents
                is ExtrinsicDispatch.Failed -> dispatch.emittedEvents
                ExtrinsicDispatch.Unknown -> emptyList()
            }
            val outcome = if (target != null && coldkey != null) {
                bindBittensorOperationOutcome(events, target, coldkey, payer, execution.blockHash)
            } else {
                null
            }
            if (result is BittensorOperationResult.Success) positionUpdates.confirmed(metaId, chain.id, coldkey)
            when (result) {
                is BittensorOperationResult.Success -> result.copy(outcome = outcome)
                // Failed calls may still have paid fees; never expose their trade events as executed amounts.
                is BittensorOperationResult.Failure -> result.copy(outcome = outcome?.copy(tao = null, alpha = null, serviceFee = null))
                is BittensorOperationResult.Unknown -> result
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.mapBittensorSubmissionError()
        }
    }
}
