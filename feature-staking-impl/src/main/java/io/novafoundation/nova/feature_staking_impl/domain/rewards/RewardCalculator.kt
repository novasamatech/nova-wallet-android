package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.common.utils.median
import io.novafoundation.nova.common.utils.sumByBigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow

private const val PARACHAINS_ENABLED = false

private const val MINIMUM_INFLATION = 0.025

private val INFLATION_IDEAL = if (PARACHAINS_ENABLED) 0.2 else 0.1
private val STAKED_PORTION_IDEAL = if (PARACHAINS_ENABLED) 0.5 else 0.75

private val INTEREST_IDEAL = INFLATION_IDEAL / STAKED_PORTION_IDEAL

private const val DECAY_RATE = 0.05

private val IGNORED_COMMISSION_THRESHOLD = 1.toBigDecimal()

const val DAYS_IN_YEAR = 365

class PeriodReturns(
    val gainAmount: BigDecimal,
    val gainFraction: BigDecimal,
)

class RewardCalculator(
    val validators: List<RewardCalculationTarget>,
    val totalIssuance: BigInteger,
) {

    private val totalStaked = validators.sumByBigInteger(RewardCalculationTarget::totalStake).toDouble()

    private val stakedPortion = totalStaked / totalIssuance.toDouble()

    private val yearlyInflation = calculateYearlyInflation()

    private val averageValidatorStake = totalStaked / validators.size

    private val averageValidatorRewardPercentage = yearlyInflation / stakedPortion

    private val apyByValidator = validators.associateBy(
        keySelector = RewardCalculationTarget::accountIdHex,
        valueTransform = ::calculateValidatorAPY
    )

    private val _expectedAPY = calculateExpectedAPY()
    private val _maxAPY = apyByValidator.values.maxOrNull() ?: 0.0

    private fun calculateExpectedAPY(): Double {
        val medianCommission = validators
            .filter { it.commission < IGNORED_COMMISSION_THRESHOLD }
            .map { it.commission.toDouble() }
            .median()

        return averageValidatorRewardPercentage * (1 - medianCommission)
    }

    private fun calculateValidatorAPY(validator: RewardCalculationTarget): Double {
        val yearlyRewardPercentage = averageValidatorRewardPercentage * averageValidatorStake / validator.totalStake.toDouble()

        return yearlyRewardPercentage * (1 - validator.commission.toDouble())
    }

    private fun calculateYearlyInflation(): Double {
        return MINIMUM_INFLATION + if (stakedPortion in 0.0..STAKED_PORTION_IDEAL) {
            stakedPortion * (INTEREST_IDEAL - MINIMUM_INFLATION / STAKED_PORTION_IDEAL)
        } else {
            (INTEREST_IDEAL * STAKED_PORTION_IDEAL - MINIMUM_INFLATION) * 2.0.pow((STAKED_PORTION_IDEAL - stakedPortion) / DECAY_RATE)
        }
    }

    val expectedAPY = _expectedAPY.toBigDecimal()

    fun getApyFor(targetIdHex: String): BigDecimal {
        val apy = apyByValidator[targetIdHex] ?: _expectedAPY

        return apy.toBigDecimal()
    }

    suspend fun calculateReturns(
        amount: BigDecimal,
        days: Int,
        isCompound: Boolean,
    ) = withContext(Dispatchers.Default) {
        val dailyPercentage = _maxAPY / DAYS_IN_YEAR

        calculateReward(amount.toDouble(), days, dailyPercentage, isCompound)
    }

    suspend fun calculateReturns(
        amount: Double,
        days: Int,
        isCompound: Boolean,
        targetIdHex: String
    ) = withContext(Dispatchers.Default) {
        val validatorAPY = apyByValidator[targetIdHex] ?: error("Validator with $targetIdHex was not found")
        val dailyPercentage = validatorAPY / DAYS_IN_YEAR

        calculateReward(amount, days, dailyPercentage, isCompound)
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
            gainFraction = gainPercentage.toBigDecimal()
        )
    }

    private fun calculateCompoundPercentage(days: Int, dailyPercentage: Double): Double {
        return (1 + dailyPercentage).pow(days) - 1
    }

    private fun calculateSimplePercentage(days: Int, dailyPercentage: Double): Double {
        return dailyPercentage * days
    }
}

suspend fun RewardCalculator.maxCompoundAPY() = calculateMaxPeriodReturns(DAYS_IN_YEAR)

suspend fun RewardCalculator.calculateMaxPeriodReturns(
    days: Int,
) = calculateReturns(
    amount = BigDecimal.ONE,
    days = days,
    isCompound = true,
).gainFraction
