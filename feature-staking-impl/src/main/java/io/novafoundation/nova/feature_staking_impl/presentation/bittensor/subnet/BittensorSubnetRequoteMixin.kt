package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetStakeOperation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSlippage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakeDraft
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn

/** As on iOS, the subnet stake setup and review re-quote the purchase on every new block while they are in front. */
class BittensorSubnetRequoteMixin @Inject constructor(
    private val chainStateRepository: ChainStateRepository,
    private val subnetInteractor: BittensorSubnetInteractor,
    private val logger: Logger
) {

    private companion object {

        const val BLOCKS_RESUBSCRIBE_DELAY_MILLIS = 5_000L
    }

    /** Each block after the current one while [active] holds, and each of [immediate]; a lost block subscription is restored after a short pause. */
    fun requotes(scope: CoroutineScope, active: () -> Boolean, immediate: Flow<Unit>): Flow<Unit> = merge(
        newBlocks().filter { active() },
        immediate
    )
        .inBackground()
        .shareIn(scope, SharingStarted.Eagerly)

    /** [draft] quoted again on each of [requotes]; a failed or timed-out re-quote is reported to [reads] and emitted as a failure. */
    fun requoted(
        draft: BittensorSubnetStakeDraft,
        slippage: BittensorSlippage,
        requotes: Flow<Unit>,
        reads: BittensorReadAnalytics
    ): Flow<Result<BittensorSubnetStakeDraft>> = requotes.mapLatest {
        try {
            val operation = draft.operation
            val quote = subnetInteractor.quote(operation.quote.subnetId, operation.quote.totalTaoInPlanks, slippage)
            Result.success(draft.copy(operation = BittensorSubnetStakeOperation(quote, operation.hotkey, operation.beneficiary)))
        } catch (cause: Exception) {
            if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
            logger.log("[BittensorSubnetStake] Block re-quote failed")
            logger.log(cause)
            reads.failed(cause)
            Result.failure(cause)
        }
    }

    private fun newBlocks(): Flow<Unit> = flowOfAll { chainStateRepository.currentRemoteBlockNumberFlow(ChainGeneses.BITTENSOR) }
        .drop(1)
        .map { }
        .retryWhen { cause, _ ->
            if (cause is CancellationException) throw cause
            delay(BLOCKS_RESUBSCRIBE_DELAY_MILLIS)
            true
        }
}
