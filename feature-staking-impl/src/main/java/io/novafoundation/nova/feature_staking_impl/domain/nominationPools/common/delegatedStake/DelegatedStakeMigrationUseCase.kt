package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.delegatedStake

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.migrateDelegation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolDelegatedStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.shouldMigrateToDelegatedStake
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

interface DelegatedStakeMigrationUseCase {

    context(ExtrinsicBuilder)
    suspend fun migrateToDelegatedStakeIfNeeded()
}

class RealDelegatedStakeMigrationUseCase(
    private val delegatedStakeRepository: NominationPoolDelegatedStakeRepository,
    private val stakingSharedState: StakingSharedState,
    private val accountRepository: AccountRepository
) : DelegatedStakeMigrationUseCase {

    context(ExtrinsicBuilder)
    override suspend fun migrateToDelegatedStakeIfNeeded() {
        val chain = stakingSharedState.chain()
        val account = accountRepository.getSelectedMetaAccount()
        val accountId = account.requireAccountIdIn(chain)

        if (delegatedStakeRepository.shouldMigrateToDelegatedStake(stakingSharedState.chainId(), accountId)) {
            nominationPools.migrateDelegation(accountId)
        }
    }
}
