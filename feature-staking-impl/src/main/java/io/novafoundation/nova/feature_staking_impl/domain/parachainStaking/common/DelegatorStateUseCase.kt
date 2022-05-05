package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class DelegatorStateUseCase(
    private val delegatorStateRepository: DelegatorStateRepository,
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
}
