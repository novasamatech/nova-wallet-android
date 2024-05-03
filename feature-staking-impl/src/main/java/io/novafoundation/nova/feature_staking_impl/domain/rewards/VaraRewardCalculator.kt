package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.common.utils.Perbill
import java.math.BigInteger

class VaraRewardCalculator(
    validators: List<RewardCalculationTarget>,
    totalIssuance: BigInteger,
    private val inflation: Perbill
) : InflationBasedRewardCalculator(validators, totalIssuance) {

    override fun calculateYearlyInflation(stakedPortion: Double): Double {
        return inflation.value
    }
}
