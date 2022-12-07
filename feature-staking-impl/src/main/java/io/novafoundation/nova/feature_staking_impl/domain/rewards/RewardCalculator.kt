package io.novafoundation.nova.feature_staking_impl.domain.rewards

import java.math.BigDecimal

class PeriodReturns(
    val gainAmount: BigDecimal,
    val gainFraction: BigDecimal,
)

interface RewardCalculator {

    val expectedAPY: BigDecimal

    val maxAPY: Double

    fun getApyFor(targetIdHex: String): BigDecimal
}
