package io.novafoundation.nova.feature_staking_impl.domain.rewards

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.math.exp
import kotlin.math.pow

const val DAYS_IN_YEAR = 365

suspend fun RewardCalculator.calculateMaxReturns(
    amount: BigDecimal,
    days: Int,
    isCompound: Boolean,
) = withContext(Dispatchers.Default) {
    val dailyPercentage = (maxAPY + 1).pow(1.0 / DAYS_IN_YEAR) - 1

    calculateReward(amount.toDouble(), days, dailyPercentage, isCompound)
}

private fun calculateReward(
    amount: Double,
    days: Int,
    dailyPercentage: Double,
    isCompound: Boolean
): PeriodReturns {
    val gainPercentage = if (isCompound) {
        calculateCompoundPercentage(days, dailyPercentage)
    } else {
        calculateSimplePercentage(days, dailyPercentage)
    }

    val gainAmount = gainPercentage * amount

    return PeriodReturns(
        gainAmount = gainAmount.toBigDecimal(),
        gainFraction = gainPercentage.toBigDecimal(),
        isCompound = isCompound
    )
}

private fun calculateCompoundPercentage(days: Int, dailyPercentage: Double): Double {
    return (1 + dailyPercentage).pow(days) - 1
}

private fun calculateSimplePercentage(days: Int, dailyPercentage: Double): Double {
    return dailyPercentage * days
}

suspend fun RewardCalculator.calculateMaxPeriodReturns(
    days: Int,
) = calculateMaxReturns(
    amount = BigDecimal.ONE,
    days = days,
    isCompound = true,
).gainFraction

fun aprToApy(apr: Double) = exp(apr) - 1.0
