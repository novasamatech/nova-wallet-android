package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards

import io.novafoundation.nova.common.utils.percentageToFraction
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.Perbill
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.InflationInfo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.ParachainBondConfig
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigDecimal
import java.math.BigInteger

class ParachainStakingRewardTarget(
    val totalStake: BigInteger,
    val accountIdHex: String
)

interface ParachainStakingRewardCalculator {

    fun averageApr(): BigDecimal

    fun maximumGain(days: Int): BigDecimal

    fun collatorApr(collatorIdHex: String): BigDecimal?

    fun calculateCollatorAnnualReturns(collatorId: AccountId, amount: BigDecimal): PeriodReturns

    fun calculateMaxAnnualReturns(amount: BigDecimal): PeriodReturns
}

private const val DAYS_IN_YEAR = 365

class RealParachainStakingRewardCalculator(
    private val bondConfig: ParachainBondConfig,
    inflationInfo: InflationInfo,
    totalIssuance: BigInteger,
    totalStaked: BigInteger,
    collators: List<ParachainStakingRewardTarget>,
    private val collatorCommission: Perbill
) : ParachainStakingRewardCalculator {

    private val stakedPortion = totalStaked.toDouble() / totalIssuance.toDouble()

    private val annualInflation = when {
        totalStaked < inflationInfo.expect.min -> inflationInfo.annual.min
        totalStaked > inflationInfo.expect.max -> inflationInfo.annual.max
        else -> inflationInfo.annual.ideal
    }

    private val annualReturn = annualInflation.toDouble() / stakedPortion

    private val averageStake = collators.map { it.totalStake.toDouble() }.average()

    private val aprByCollator = collators.associateBy(
        keySelector = ParachainStakingRewardTarget::accountIdHex,
        valueTransform = ::calculateCollatorApr
    )

    private val averageApr = calculatorApr(collatorStake = averageStake)

    private val maxApr = aprByCollator.values.maxOrNull() ?: 0.0

    override fun averageApr(): BigDecimal {
        return averageApr.toBigDecimal()
    }

    override fun maximumGain(days: Int): BigDecimal {
        return (maxApr * days / DAYS_IN_YEAR).toBigDecimal()
    }

    override fun collatorApr(collatorIdHex: String): BigDecimal? {
        return aprByCollator[collatorIdHex]?.toBigDecimal()
    }

    override fun calculateCollatorAnnualReturns(collatorId: AccountId, amount: BigDecimal): PeriodReturns {
        val collatorApr = collatorApr(collatorId.toHexString()) ?: averageApr()

        return PeriodReturns(
            gainAmount = amount * collatorApr,
            gainFraction = collatorApr
        )
    }

    override fun calculateMaxAnnualReturns(amount: BigDecimal): PeriodReturns {
        val averageApr = maximumAnnualApr()

        return PeriodReturns(
            gainAmount = amount * averageApr,
            gainFraction = averageApr
        )
    }

    private fun calculateCollatorApr(collator: ParachainStakingRewardTarget): Double {
        return calculatorApr(collator.totalStake.toDouble())
    }

    private fun calculatorApr(collatorStake: Double): Double {
        return annualReturn * (1 - bondConfig.percentageAsFraction() - collatorCommission.toDouble()) * (averageStake / collatorStake)
    }

    private fun ParachainBondConfig.percentageAsFraction() = percent.toDouble().percentageToFraction()
}

fun ParachainStakingRewardCalculator.maximumAnnualApr() = maximumGain(DAYS_IN_YEAR)
