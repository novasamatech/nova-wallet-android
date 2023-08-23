package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.nomination

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.NominationPoolsAvailableBalanceResolver
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

class NominationPoolStartStakingInteractor(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope,
    private val poolsAvailableBalanceResolver: NominationPoolsAvailableBalanceResolver,
) : StartStakingInteractor {

    override fun observeData(): Flow<StartStakingData> {
        return flowOf {
            val rewardCalculator = nominationPoolSharedComputation.poolRewardCalculator(stakingOption, scope)
            val minJoinBond = nominationPoolSharedComputation.minJoinBond(stakingOption.chain.id, scope)

            StartStakingData(
                maxEarningRate = rewardCalculator.maxAPY,
                minStake = minJoinBond,
                payoutType = PayoutType.Manual,
                participationInGovernance = false
            )
        }
    }

    override suspend fun getAvailableBalance(asset: Asset): BigInteger {
        return poolsAvailableBalanceResolver.availableBalanceToStartStaking(asset)
    }
}
