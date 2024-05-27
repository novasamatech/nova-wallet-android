package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.common.utils.Perbill
import java.math.BigInteger

class VaraRewardCalculator(
    validators: List<RewardCalculationTarget>,
    totalIssuance: BigInteger,
    private val inflation: Perbill
) : InflationBasedRewardCalculator(validators, totalIssuance) {

    override fun calculateYearlyInflation(stakedPortion: Double): Double {
        // When calculating era payout, Vara runtime simply divides yearly payout by number of eras in the year
        // Which results in `inflation` to correspond to simple returns (APR)
        // So, we adjust it to compound returns (APY)
        return aprToApy(inflation.value)
    }
}
