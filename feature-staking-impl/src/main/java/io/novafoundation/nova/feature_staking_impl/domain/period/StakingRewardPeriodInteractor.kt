package io.novafoundation.nova.feature_staking_impl.domain.period

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.components
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

interface StakingRewardPeriodInteractor {

    suspend fun setRewardPeriod(stakingOption: StakingOption, rewardPeriod: RewardPeriod)

    suspend fun getRewardPeriod(stakingOption: StakingOption): RewardPeriod

    fun observeRewardPeriod(stakingOption: StakingOption): Flow<RewardPeriod>
}

class RealStakingRewardPeriodInteractor(
    private val stakingPeriodRepository: StakingPeriodRepository,
    private val accountRepository: AccountRepository
) : StakingRewardPeriodInteractor {

    override suspend fun setRewardPeriod(stakingOption: StakingOption, rewardPeriod: RewardPeriod) {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val (chain, asset, stakingType) = stakingOption.components
        val accountId = metaAccount.accountIdIn(chain) ?: return
        stakingPeriodRepository.setRewardPeriod(accountId, chain, asset, stakingType, rewardPeriod)
    }

    override suspend fun getRewardPeriod(stakingOption: StakingOption): RewardPeriod {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val (chain, asset, stakingType) = stakingOption.components
        val accountId = metaAccount.accountIdIn(chain) ?: return RewardPeriod.AllTime
        return stakingPeriodRepository.getRewardPeriod(accountId, chain, asset, stakingType)
    }

    override fun observeRewardPeriod(stakingOption: StakingOption): Flow<RewardPeriod> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest {
            val (chain, asset, stakingType) = stakingOption.components
            val accountId = it.accountIdIn(stakingOption.assetWithChain.chain) ?: return@flatMapLatest emptyFlow()
            stakingPeriodRepository.observeRewardPeriod(accountId, chain, asset, stakingType)
        }
    }
}
