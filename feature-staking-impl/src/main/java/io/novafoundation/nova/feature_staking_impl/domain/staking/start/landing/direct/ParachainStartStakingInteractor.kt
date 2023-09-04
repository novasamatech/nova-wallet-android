package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.direct

import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.rewards.DAYS_IN_YEAR
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class ParachainStartStakingInteractor(
    private val parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
    private val parachainStakingRewardCalculator: ParachainStakingRewardCalculator,
    private val stakingOption: StakingOption,
) : StartStakingInteractor {

    override fun observeData(): Flow<StartStakingData> {
        val chain = stakingOption.chain

        return parachainNetworkInfoInteractor.observeRoundInfo(chain.id).map { activeEraInfo ->
            StartStakingData(
                maxEarningRate = parachainStakingRewardCalculator.maximumGain(DAYS_IN_YEAR).asPerbill(),
                minStake = activeEraInfo.minimumStake,
                payoutType = getPayoutType(),
                participationInGovernance = chain.governance.isNotEmpty()
            )
        }
    }

    override suspend fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.freeInPlanks
    }

    private fun getPayoutType(): PayoutType {
        return PayoutType.Automatic.Payout
    }
}