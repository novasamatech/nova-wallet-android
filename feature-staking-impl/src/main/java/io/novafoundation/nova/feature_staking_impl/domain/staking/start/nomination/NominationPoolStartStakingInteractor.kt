package io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class NominationPoolStartStakingInteractor(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope,
) : StartStakingInteractor {

    override fun observeData(): Flow<StartStakingData> {
        return flowOfAll {
            val rewardCalculator = nominationPoolSharedComputation.poolRewardCalculator(stakingOption, scope)

            nominationPoolGlobalsRepository.observeMinJoinBond(stakingOption.chain.id)
                .map { minJoinBond ->
                    StartStakingData(
                        maxEarningRate = rewardCalculator.maxAPY.toBigDecimal(),
                        minStake = minJoinBond,
                        payoutType = PayoutType.Manual,
                        participationInGovernance = false
                    )
                }
        }
    }

    override fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.transferableInPlanks
    }
}
