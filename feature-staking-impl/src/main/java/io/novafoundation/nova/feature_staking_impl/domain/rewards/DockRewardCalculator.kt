package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.common.utils.Perbill
import java.math.BigInteger

class DockRewardCalculator(
    validators: List<RewardCalculationTarget>,
    private val totalIssuance: BigInteger,
    private val yearlyEmission: BigInteger,
    private val treasuryRewardsPercentage: Perbill,
): InflationBasedRewardCalculator(validators, totalIssuance) {

    override fun calculateYearlyInflation(stakedPortion: Double): Double {
        val inflation = yearlyEmission.toDouble() / totalIssuance.toDouble()

        return inflation * (1 - treasuryRewardsPercentage.value)
    }
}
