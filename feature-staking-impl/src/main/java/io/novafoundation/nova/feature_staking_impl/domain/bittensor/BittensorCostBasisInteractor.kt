package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorHistoryRequest
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorOperationHistoryRepository
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface BittensorCostBasisState {
    object Unavailable : BittensorCostBasisState
    object Loading : BittensorCostBasisState
    data class Loaded(val basis: BittensorEstimatedCostBasis) : BittensorCostBasisState
    data class Failed(val cause: Throwable) : BittensorCostBasisState
}

data class BittensorCostBasisUpdate(val state: BittensorCostBasisState, val isSettling: Boolean)

class BittensorCostBasisInteractor(
    private val historyRepository: BittensorOperationHistoryRepository,
    private val accountRepository: AccountRepository,
    private val logger: Logger
) {

    fun observe(context: Flow<BittensorEstimateContext?>): Flow<BittensorCostBasisState> = observeUpdates(context).map { it.state }

    fun observeUpdates(context: Flow<BittensorEstimateContext?>): Flow<BittensorCostBasisUpdate> =
        context.distinctUntilChanged()
            .flatMapLatest { current ->
                if (current == null) flowOf(BittensorCostBasisUpdate(BittensorCostBasisState.Unavailable, false)) else observeAccount(current)
            }

    suspend fun ownTradeRequest(metaId: Long, chain: Chain): BittensorHistoryRequest? {
        return try {
            val wallet = accountRepository.getMetaAccount(metaId)
            historyRepository.request(chain.addressOf(wallet.requireAccountIdIn(chain)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorCostBasis] Could not capture optional history cache context")
            logger.log(e)
            null
        }
    }

    fun onTradeResult(request: BittensorHistoryRequest?, result: BittensorOperationResult) {
        val submitted = when (result) {
            is BittensorOperationResult.Success, is BittensorOperationResult.Unknown -> true
            is BittensorOperationResult.Failure -> result.stage == BittensorFailureStage.UNKNOWN
        }
        if (request != null && submitted) {
            historyRepository.ownTradeSubmitted(request)
        }
    }

    private fun observeAccount(context: BittensorEstimateContext): Flow<BittensorCostBasisUpdate> =
        accountRepository.metaAccountFlow(context.metaId)
            .map { AccountIdKey(it.requireAccountIdIn(context.chain)) }
            .distinctUntilChanged()
            .flatMapLatest { coldKey ->
                historyRepository.observeState(context.chain.addressOf(coldKey.value))
                    .map { update ->
                        val state = update.result?.fold(
                            onSuccess = { BittensorCostBasisState.Loaded(BittensorEstimatedCostBasis(it.operations, it.allProviderPagesRead)) },
                            onFailure = { BittensorCostBasisState.Failed(it) }
                        ) ?: BittensorCostBasisState.Loading
                        BittensorCostBasisUpdate(state, update.isSettling)
                    }
                    .onStart { emit(BittensorCostBasisUpdate(BittensorCostBasisState.Loading, false)) }
            }
}
