package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.feature_staking_api.domain.model.InflationPredictionInfo
import io.novafoundation.nova.feature_staking_api.domain.model.calculateStakersInflation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlin.time.Duration

class InflationPredictionInfoCalculator(
    private val inflationPredictionInfo: InflationPredictionInfo,
    private val eraDuration: Duration,
    private val totalIssuance: Balance,
    validators: List<RewardCalculationTarget>
) : InflationBasedRewardCalculator(validators, totalIssuance) {

    override fun calculateYearlyInflation(stakedPortion: Double): Double {
        return inflationPredictionInfo.calculateStakersInflation(totalIssuance, eraDuration)
    }
}
