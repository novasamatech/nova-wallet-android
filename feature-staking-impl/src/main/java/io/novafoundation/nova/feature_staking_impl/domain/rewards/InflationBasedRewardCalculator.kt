package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.common.utils.median
import io.novafoundation.nova.common.utils.sumByBigInteger
import java.math.BigDecimal
import java.math.BigInteger

private val IGNORED_COMMISSION_THRESHOLD = 1.toBigDecimal()

abstract class InflationBasedRewardCalculator(
    private val validators: List<RewardCalculationTarget>,
    private val totalIssuance: BigInteger,
) : RewardCalculator {

    abstract fun calculateYearlyInflation(stakedPortion: Double): Double

    private val totalStaked by lazy {
        validators.sumByBigInteger(RewardCalculationTarget::totalStake).toDouble()
    }

    private val averageValidatorRewardPercentage by lazy {
        val stakedPortion = totalStaked / totalIssuance.toDouble()
        val yearlyInflation = calculateYearlyInflation(stakedPortion)

        yearlyInflation / stakedPortion
    }

    private val apyByValidator by lazy {
        val averageValidatorStake = totalStaked / validators.size

        validators.associateBy(
            keySelector = RewardCalculationTarget::accountIdHex,
            valueTransform = { calculateValidatorAPY(it, averageValidatorRewardPercentage, averageValidatorStake) }
        )
    }

    override val expectedAPY by lazy {
        calculateExpectedAPY(averageValidatorRewardPercentage).toBigDecimal()
    }

    override val maxAPY by lazy {
        apyByValidator.values.maxOrNull() ?: 0.0
    }

    private fun calculateValidatorAPY(
        validator: RewardCalculationTarget,
        averageValidatorRewardPercentage: Double,
        averageValidatorStake: Double,
    ): Double {
        val yearlyRewardPercentage = averageValidatorRewardPercentage * averageValidatorStake / validator.totalStake.toDouble()

        return yearlyRewardPercentage * (1 - validator.commission.toDouble())
    }

    private fun calculateExpectedAPY(
        averageValidatorRewardPercentage: Double
    ): Double {
        val medianCommission = validators
            .filter { it.commission < IGNORED_COMMISSION_THRESHOLD }
            .map { it.commission.toDouble() }
            .median()

        return averageValidatorRewardPercentage * (1 - medianCommission)
    }

    override fun getApyFor(targetIdHex: String): BigDecimal {
        return apyByValidator[targetIdHex]?.toBigDecimal() ?: expectedAPY
    }
}
