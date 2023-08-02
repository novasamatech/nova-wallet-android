package io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct

import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.rewards.DAYS_IN_YEAR
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParachainStartStakingInteractor(
    private val parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
    private val parachainStakingRewardCalculator: ParachainStakingRewardCalculator
) : StartStakingInteractor {

    override fun observeData(chain: Chain, asset: Asset): Flow<StartStakingData> {
        return parachainNetworkInfoInteractor.observeRoundInfo(chain.id).map { activeEraInfo ->
            StartStakingData(
                availableBalance = getAvailableBalance(asset),
                maxEarningRate = parachainStakingRewardCalculator.maximumGain(DAYS_IN_YEAR),
                minStake = activeEraInfo.minimumStake,
                payoutType = getPayoutType(),
                participationInGovernance = chain.governance.isNotEmpty()
            )
        }
    }

    private fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.freeInPlanks
    }

    private fun getPayoutType(): PayoutType {
        return PayoutType.Automatic.Payout
    }
}
