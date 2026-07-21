package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Computes the staking return from the staking pallet's per-era reward allocation
 * (`EraRewardAllocation.stakerRewards`, which mirrors `Staking.ErasValidatorReward`).
 *
 * After the Dynamic Allocation Pool (DAP) reform the network no longer mints the whole period
 * issuance to stakers: of the ~153,132 DOT minted per day only the staker allocation (currently
 * 45.2% ≈ 69,216 DOT/day) is paid to stakers — the rest goes to the validator incentive and the
 * DAP buffer. Reading the recorded era allocation keeps the APY correct without hardcoding the
 * split: it tracks both the issuance curve (ref 1710) and any future governance re-allocation
 * of the DAP budget.
 *
 * The yearly inflation is expressed relative to total issuance because
 * [InflationBasedRewardCalculator] divides it by the staked portion
 * (= totalStaked / totalIssuance) — issuance cancels and the effective staker return reduces to
 * `stakersEraReward * erasInYear / totalStaked`.
 */
class EraRewardAllocationRewardCalculator(
    private val stakersEraReward: Balance,
    private val eraDuration: Duration,
    private val totalIssuance: Balance,
    validators: List<RewardCalculationTarget>
) : InflationBasedRewardCalculator(validators, totalIssuance) {

    override fun calculateYearlyInflation(stakedPortion: Double): Double {
        val erasInYear = (365.days / eraDuration).roundToInt()
        val inflationPerEra = stakersEraReward.divideToDecimal(totalIssuance)

        return inflationPerEra.toDouble() * erasInYear
    }
}
