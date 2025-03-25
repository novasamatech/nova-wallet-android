package io.novafoundation.nova.feature_staking_impl.domain.mythos.main.userRewards

import android.util.Log
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.fullId
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.userRewards.MythosUserRewardsInteractor.MythosRewards
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

interface MythosUserRewardsInteractor {

    class MythosRewards(
        val total: LoadingState<Balance>,
        val claimable: LoadingState<Balance>
    )

    fun rewardsFlow(stakingOption: StakingOption): Flow<MythosRewards>

    suspend fun syncTotalRewards(stakingOption: StakingOption, rewardPeriod: RewardPeriod): Result<Unit>
}

@FeatureScope
class RealMythosUserRewardsInteractor @Inject constructor(
    private val repository: MythosUserStakeRepository,
    private val stakingRewardsRepository: StakingRewardsRepository,
    private val accountRepository: AccountRepository,
) : MythosUserRewardsInteractor {

    override fun rewardsFlow(stakingOption: StakingOption): Flow<MythosRewards> {
        return flowOfAll {
            val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(stakingOption.chain)

            combine(
                pendingRewardsFlow(accountId, stakingOption.chain.id).withLoading(),
                stakingRewardsRepository.totalRewardFlow(accountId, stakingOption.fullId).withLoading()
            ) { pendingRewards, totalRewards ->
                MythosRewards(
                    total = totalRewards,
                    claimable = pendingRewards,
                )
            }
        }
    }

    override suspend fun syncTotalRewards(stakingOption: StakingOption, rewardPeriod: RewardPeriod): Result<Unit> {
        return runCatching {
            val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(stakingOption.chain)

            stakingRewardsRepository.sync(accountId, stakingOption, rewardPeriod)
        }
    }

    private fun pendingRewardsFlow(accountId: AccountId, chainId: ChainId): Flow<Balance> {
        return flowOf { repository.getpPendingRewards(chainId, accountId.intoKey()) }
            .catch { Log.e("RealMythosUserRewardsInteractor", "Failed to fetch pending rewards", it) }
    }
}
