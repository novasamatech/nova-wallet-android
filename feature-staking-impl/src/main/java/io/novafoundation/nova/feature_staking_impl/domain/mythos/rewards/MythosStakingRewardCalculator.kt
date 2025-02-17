package io.novafoundation.nova.feature_staking_impl.domain.mythos.rewards

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

interface MythosStakingRewardCalculator {

    val maxApr: Fraction

    fun collatorApr(collatorId: AccountIdKey): Fraction?

    fun calculateCollatorAnnualReturns(collatorId: AccountIdKey, amount: BigDecimal): PeriodReturns

    fun calculateMaxAnnualReturns(amount: BigDecimal): PeriodReturns
}

/**
 * Implementation based on the following derivation:
 *
 * x - user stake
 * T - a particular collator's current total stake
 * Cn - number of collators
 * e - per-collator emission (in tokens)
 * E - total emission
 * user_yield - user yield per session, in %, for a particular collator
 *
 * e = E / Cn
 * staked_portion = x / (x + T)
 * user_yield (in %) = staked_portion * e / x = e / (x + T)
 *
 * We use min stake for x to not face enormous numbers when total stake in the system is close to zero
 */
class RealMythosStakingRewardCalculator(
    private val perBlockRewards: Balance,
    private val blockDuration: Duration,
    private val collatorCommission: Fraction,
    private val collators: List<MythosStakingRewardTarget>,
    private val minStake: Balance
) : MythosStakingRewardCalculator {

    private val yearlyEmission = calculateYearlyEmission()
    private val collatorCommissionFraction = collatorCommission.inFraction

    private val aprByCollator = collators.associateBy(
        keySelector = MythosStakingRewardTarget::accountId,
        valueTransform = ::calculateCollatorApr
    )

    private val _maxApr = aprByCollator.values.maxOrNull().orZero()
    override val maxApr: Fraction = _maxApr.fractions

    override fun collatorApr(collatorId: AccountIdKey): Fraction? {
        return aprByCollator[collatorId]?.fractions
    }

    override fun calculateCollatorAnnualReturns(collatorId: AccountIdKey, amount: BigDecimal): PeriodReturns {
        val collatorApr = collatorApr(collatorId) ?: error("Collator $collatorId not found")
        val aprFraction = collatorApr.inFraction.toBigDecimal()

        return PeriodReturns(
            gainAmount = amount * aprFraction,
            gainFraction = aprFraction,
            isCompound = false
        )
    }

    override fun calculateMaxAnnualReturns(amount: BigDecimal): PeriodReturns {
        val maxApr = _maxApr.toBigDecimal()

        return PeriodReturns(
            gainAmount = amount * maxApr,
            gainFraction = maxApr,
            isCompound = false
        )
    }

    private fun calculateYearlyEmission(): Double {
        val blocksPerYear = (365.days / blockDuration).toInt()
        return perBlockRewards.toDouble() * blocksPerYear
    }

    private fun calculateCollatorApr(collator: MythosStakingRewardTarget): Double {
        return calculatorApr(collator.totalStake.toDouble())
    }

    private fun calculatorApr(collatorStake: Double): Double {
        val perCollatorRewards = yearlyEmission / collators.size * (1 - collatorCommissionFraction)
        val minUserStake = minStake.toDouble()

        // We estimate rewards assuming user stakes at least min_stake - this will compute maximum possible APR
        // But at least not as big as when min stake not accounted
        return perCollatorRewards / (collatorStake + minUserStake)
    }
}
