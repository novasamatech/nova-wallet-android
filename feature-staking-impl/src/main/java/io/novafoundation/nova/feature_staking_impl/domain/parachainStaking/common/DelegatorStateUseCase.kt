package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class DelegatorStateUseCase(
    private val delegatorStateRepository: DelegatorStateRepository,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val accountRepository: AccountRepository,
) {

    fun delegatorStateFlow(
        account: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset
    ): Flow<DelegatorState> {
        return flow {
            val accountId = account.accountIdIn(chain)

            if (accountId != null) {
                emitAll(delegatorStateRepository.observeDelegatorState(chain, chainAsset, accountId))
            } else {
                emit(DelegatorState.None(chain))
            }
        }
    }

    fun currentDelegatorStateFlow() = accountRepository.selectedMetaAccountFlow().flatMapLatest {
        val (chain, asset) = singleAssetSharedState.chainAndAsset()

        delegatorStateFlow(it, chain, asset)
    }

    suspend fun currentDelegatorState(): DelegatorState {
        val account = accountRepository.getSelectedMetaAccount()
        val (chain, asset) = singleAssetSharedState.chainAndAsset()

        val accountId = account.accountIdIn(chain)

        return if (accountId != null) {
            delegatorStateRepository.getDelegationState(chain, asset, accountId)
        } else {
            DelegatorState.None(chain)
        }
    }
}
