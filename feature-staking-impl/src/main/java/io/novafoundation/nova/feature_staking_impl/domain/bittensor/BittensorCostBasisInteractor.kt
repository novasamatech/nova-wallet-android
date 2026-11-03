package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorOperationHistorySource
import io.novafoundation.nova.runtime.ext.addressOf
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

/** A provisional basis uses stale history or history that may not include a recently submitted trade yet. */
data class BittensorCostBasisUpdate(val state: BittensorCostBasisState, val isProvisional: Boolean)

class BittensorCostBasisInteractor(
    private val historySource: BittensorOperationHistorySource,
    private val accountRepository: AccountRepository
) {

    fun observe(context: Flow<BittensorEstimateContext?>): Flow<BittensorCostBasisState> = observeUpdates(context).map { it.state }

    fun observeUpdates(context: Flow<BittensorEstimateContext?>): Flow<BittensorCostBasisUpdate> =
        context.distinctUntilChanged()
            .flatMapLatest { current ->
                if (current == null) {
                    flowOf(BittensorCostBasisUpdate(state = BittensorCostBasisState.Unavailable, isProvisional = false))
                } else {
                    observeAccount(current)
                }
            }

    private fun observeAccount(context: BittensorEstimateContext): Flow<BittensorCostBasisUpdate> =
        accountRepository.metaAccountFlow(context.metaId)
            .map { AccountIdKey(it.requireAccountIdIn(context.chain)) }
            .distinctUntilChanged()
            .flatMapLatest { coldKey ->
                historySource.observeState(context.chain.addressOf(coldKey.value), context.metaId)
                    .map { update ->
                        val state = update.result?.fold(
                            onSuccess = { BittensorCostBasisState.Loaded(BittensorEstimatedCostBasis(it.operations, it.allProviderPagesRead)) },
                            onFailure = { BittensorCostBasisState.Failed(it) }
                        ) ?: BittensorCostBasisState.Loading
                        val isHistoryStale = update.result?.getOrNull()?.stale == true
                        val isAwaitingIndexedTrade = update.isSettling
                        BittensorCostBasisUpdate(
                            state = state,
                            isProvisional = isAwaitingIndexedTrade || isHistoryStale
                        )
                    }
                    .onStart { emit(BittensorCostBasisUpdate(state = BittensorCostBasisState.Loading, isProvisional = false)) }
            }
}
