package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

internal object BittensorWithdrawalBounds {

    val PER_U16_DENOMINATOR: BigInteger = BigInteger.valueOf(65_535)

    val FLOOR_MARGIN_DENOMINATOR: BigInteger = BigInteger.valueOf(100)

    val DEFAULT_SWAP_FEE_RATE: BigInteger = BigInteger.valueOf(33)
}

internal fun bittensorWithdrawalFloor(minimumTao: BigInteger, price: BigInteger, feeRate: BigInteger): BigInteger {
    val denominator = BittensorWithdrawalBounds.PER_U16_DENOMINATOR
    require(price.isPositive() && feeRate.isNonNegative && feeRate < denominator)
    val divisor = price * (denominator - feeRate)
    val numerator = minimumTao * BittensorProtocolUnits.RAO_PER_TAO * denominator
    val floor = (numerator + divisor - BigInteger.ONE) / divisor
    val marginDenominator = BittensorWithdrawalBounds.FLOOR_MARGIN_DENOMINATOR
    val margin = (floor + marginDenominator - BigInteger.ONE) / marginDenominator
    return floor + margin
}

/**
 * Subtensor `get_nominator_min_required_stake`: `NominatorMinRequiredStake` is a per-million factor of
 * `DefaultMinStake` (= `InitialMinStake`), not an absolute rao amount. A non-owner remainder below it is
 * swept to the coldkey's free balance by `clear_small_nomination_if_required`.
 */
internal fun bittensorNominatorMinStake(minStake: BigInteger, perMillionFactor: BigInteger): BigInteger {
    return minStake * perMillionFactor / BittensorProtocolUnits.NOMINATOR_FACTOR_DENOMINATOR
}

data class BittensorExitSlice(val hotkey: AccountIdKey, val amount: BigInteger, val fullExit: Boolean) {

    init {
        require(hotkey.value.size == 32)
        require(amount.isPositive() && amount.bitLength() <= 64)
    }
}

data class BittensorExitMember(
    val stake: BigInteger,
    val collateral: BigInteger,
    val isOwnHotkey: Boolean,
    val holdRemaining: BigInteger
) {

    init {
        require(stake.isNonNegative && collateral.isNonNegative && holdRemaining.isNonNegative)
    }

    val free: BigInteger get() = if (holdRemaining.isPositive()) BigInteger.ZERO else (stake - collateral).max(BigInteger.ZERO)

    fun drawable(floor: BigInteger): BigInteger = if (collateral.isPositive() && free < floor) BigInteger.ZERO else free
}

/** Frozen call slices: full removals first, followed by exact partial amounts. */
data class BittensorExitPlan(val slices: List<BittensorExitSlice>) {

    init {
        require(slices.isNotEmpty())
        require(slices.map { it.hotkey }.distinct().size == slices.size)
    }

    val amount: BigInteger get() = slices.fold(BigInteger.ZERO) { total, slice -> total + slice.amount }

    val isBatched: Boolean get() = slices.size > 1
}

/** The amount exceeds what the group can withdraw at the current floor: shown as "Not enough tokens", not a generic error. */
class BittensorExitExceedsStakeException : IllegalArgumentException("Withdrawal exceeds available group stake")
