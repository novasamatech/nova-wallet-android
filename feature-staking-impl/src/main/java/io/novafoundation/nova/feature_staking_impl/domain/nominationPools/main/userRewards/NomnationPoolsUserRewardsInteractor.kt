package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.userRewards

import android.util.Log
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.userRewards.NominationPoolsUserRewardsInteractor.NominationPoolRewards
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

interface NominationPoolsUserRewardsInteractor {

    class NominationPoolRewards(
        val total: LoadingState<Balance>,
        val claimable: LoadingState<Balance>
    )

    fun rewardsFlow(accountId: AccountId, stakingOptionId: StakingOptionId): Flow<NominationPoolRewards>

    suspend fun syncRewards(accountId: AccountId, stakingOption: StakingOption, rewardPeriod: RewardPeriod): Result<*>
}

class RealNominationPoolsUserRewardsInteractor(
    private val repository: NominationPoolMembersRepository,
    private val stakingRewardsRepository: StakingRewardsRepository,
) : NominationPoolsUserRewardsInteractor {

    override fun rewardsFlow(accountId: AccountId, stakingOptionId: StakingOptionId): Flow<NominationPoolRewards> {
        return combine(
            pendingRewardsFlow(accountId, stakingOptionId.chainId).withLoading(),
            stakingRewardsRepository.totalRewardFlow(accountId, stakingOptionId).withLoading()
        ) { pendingRewards, totalRewards ->
            NominationPoolRewards(
                total = totalRewards,
                claimable = pendingRewards,
            )
        }
    }

    override suspend fun syncRewards(accountId: AccountId, stakingOption: StakingOption, rewardPeriod: RewardPeriod): Result<*> {
        return runCatching { stakingRewardsRepository.sync(accountId, stakingOption, rewardPeriod) }
    }

    private fun pendingRewardsFlow(accountId: AccountId, chainId: ChainId): Flow<Balance> {
        return flowOf { repository.getPendingRewards(accountId, chainId) }
            .catch { Log.e("NominationPoolsUserRewardsInteractor", "Failed to fetch pending rewards", it) }
    }
}
