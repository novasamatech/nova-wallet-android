package io.novafoundation.nova.feature_staking_impl.domain.period

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

interface StakingRewardPeriodInteractor {

    suspend fun setRewardPeriod(chain: Chain, asset: Chain.Asset, rewardPeriod: RewardPeriod)

    suspend fun getRewardPeriod(chain: Chain, asset: Chain.Asset): RewardPeriod

    fun observeRewardPeriod(chain: Chain, asset: Chain.Asset): Flow<RewardPeriod>
}

class RealStakingRewardPeriodInteractor(
    private val stakingPeriodRepository: StakingPeriodRepository,
    private val accountRepository: AccountRepository
) : StakingRewardPeriodInteractor {

    override suspend fun setRewardPeriod(chain: Chain, asset: Chain.Asset, rewardPeriod: RewardPeriod) {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = metaAccount.accountIdIn(chain) ?: return
        stakingPeriodRepository.setRewardPeriod(accountId, chain, asset, rewardPeriod)
    }

    override suspend fun getRewardPeriod(chain: Chain, asset: Chain.Asset): RewardPeriod {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = metaAccount.accountIdIn(chain) ?: return RewardPeriod.AllTime
        return stakingPeriodRepository.getRewardPeriod(accountId, chain, asset)
    }

    override fun observeRewardPeriod(chain: Chain, asset: Chain.Asset): Flow<RewardPeriod> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest {
            val accountId = it.accountIdIn(chain) ?: return@flatMapLatest emptyFlow()
            stakingPeriodRepository.observeRewardPeriod(accountId, chain, asset)
        }
    }
}
