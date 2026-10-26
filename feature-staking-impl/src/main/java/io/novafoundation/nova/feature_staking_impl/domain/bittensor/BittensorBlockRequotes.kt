package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen

class BittensorBlockRequotes @Inject constructor(
    private val chainStateRepository: ChainStateRepository,
    private val logger: Logger
) {

    private companion object {

        const val BLOCKS_RESUBSCRIBE_DELAY_MILLIS = 5_000L
    }

    /** Each block after the current one while [active]; an inactive screen skips them and resumes with the next block. */
    fun newBlocks(chainId: ChainId, active: Flow<Boolean>): Flow<Unit> = active.distinctUntilChanged()
        .flatMapLatest { isActive ->
            if (isActive) flowOfAll { chainStateRepository.currentRemoteBlockNumberFlow(chainId) }.drop(1).map { } else emptyFlow()
        }
        .conflate()

    /** Each new block while [active] and each of [immediate]; a lost block subscription is restored after a short pause. */
    fun requotes(active: Flow<Boolean>, immediate: Flow<Unit>): Flow<Unit> = merge(
        newBlocks(ChainGeneses.BITTENSOR, active).retryWhen { cause, _ ->
            if (cause is CancellationException) throw cause
            delay(BLOCKS_RESUBSCRIBE_DELAY_MILLIS)
            true
        },
        immediate
    )

    /** A block re-quote; a failed or timed-out read fails it. */
    suspend fun <T> requote(quote: suspend () -> T): Result<T> = try {
        Result.success(quote())
    } catch (cause: Exception) {
        if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
        logger.log("[Bittensor] Block re-quote failed")
        logger.log(cause)
        Result.failure(cause)
    }
}
