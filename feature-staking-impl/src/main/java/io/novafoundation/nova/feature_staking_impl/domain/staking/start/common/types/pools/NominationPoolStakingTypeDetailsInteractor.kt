package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.pools

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

class NominationPoolStakingTypeDetailsInteractor(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope,
) : StakingTypeDetailsInteractor {

    override fun observeData(): Flow<StakingTypeDetails> {
        return flowOf {
            val rewardCalculator = nominationPoolSharedComputation.poolRewardCalculator(stakingOption, scope)
            val minJoinBond = nominationPoolSharedComputation.minJoinBond(stakingOption.chain.id, scope)

            StakingTypeDetails(
                maxEarningRate = rewardCalculator.maxAPY,
                minStake = minJoinBond,
                payoutType = PayoutType.Manual,
                participationInGovernance = false,
                advancedOptionsAvailable = false
            )
        }
    }

    override fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.transferableInPlanks
    }
}
