package io.novafoundation.nova.feature_staking_impl.domain.rewards

import java.math.BigDecimal

const val DAYS_IN_YEAR = 365

class PeriodReturns(
    val gainAmount: BigDecimal,
    val gainFraction: BigDecimal,
)

interface RewardCalculator {

    val expectedAPY: BigDecimal

    fun getApyFor(targetIdHex: String): BigDecimal

    suspend fun calculateReturns(
        amount: BigDecimal,
        days: Int,
        isCompound: Boolean,
    ): PeriodReturns
}

suspend fun RewardCalculator.maxCompoundAPY() = calculateMaxPeriodReturns(DAYS_IN_YEAR)

suspend fun RewardCalculator.calculateMaxPeriodReturns(
    days: Int,
) = calculateReturns(
    amount = BigDecimal.ONE,
    days = days,
    isCompound = true,
).gainFraction
