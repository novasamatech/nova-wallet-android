package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigDecimal

private const val DAYS_IN_YEAR = 365

class AvnParachainStakingRewardCalculator(
    annualApr: BigDecimal,
    collators: List<ParachainStakingRewardTarget>,
    private val collatorCommission: Perbill,
) : ParachainStakingRewardCalculator {

    private val annualReturn = annualApr.toDouble()

    private val averageStake = collators.map { it.totalStake.toDouble() }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?: 0.0

    private val aprByCollator = collators.associateBy(
        keySelector = ParachainStakingRewardTarget::accountIdHex,
        valueTransform = ::collatorApr
    )

    private val averageApr = aprFor(collatorStake = averageStake)

    private val maxApr = aprByCollator.values.maxOrNull() ?: 0.0

    override fun maximumGain(days: Int): BigDecimal {
        return (maxApr * days / DAYS_IN_YEAR).toBigDecimal()
    }

    override fun collatorApr(collatorIdHex: String): BigDecimal? {
        return aprByCollator[collatorIdHex]?.toBigDecimal()
    }

    override fun calculateCollatorAnnualReturns(collatorId: AccountId, amount: BigDecimal): PeriodReturns {
        val collatorApr = collatorApr(collatorId.toHexString()) ?: averageApr.toBigDecimal()

        return PeriodReturns(
            gainAmount = amount * collatorApr,
            gainFraction = collatorApr,
            isCompound = false
        )
    }

    override fun calculateMaxAnnualReturns(amount: BigDecimal): PeriodReturns {
        val maxAnnual = maximumAnnualApr()

        return PeriodReturns(
            gainAmount = amount * maxAnnual,
            gainFraction = maxAnnual,
            isCompound = false
        )
    }

    private fun collatorApr(collator: ParachainStakingRewardTarget): Double {
        return aprFor(collator.totalStake.toDouble())
    }

    private fun aprFor(collatorStake: Double): Double {
        if (collatorStake <= 0.0 || averageStake <= 0.0) return 0.0
        return annualReturn * (1 - collatorCommission.toDouble()) * (averageStake / collatorStake)
    }
}
