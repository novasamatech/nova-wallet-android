package io.novafoundation.nova.feature_staking_impl.data.subtensor.extrinsic

import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Extrinsic builder helpers for Bittensor's `SubtensorModule` staking calls.
 *
 * v1 always uses the `*_limit` variants (`add_stake_limit` /
 * `remove_stake_limit`) even on the root subnet where there is no AMM —
 * keeps call shapes consistent with the future v2 subnet path. The slippage
 * cushion is cosmetic on root and load-bearing on subnets.
 *
 * Mirrors `SubtensorExtrinsicBuilder.swift` on iOS.
 */

private const val MODULE_NAME = "SubtensorModule"
private const val ONE_TAO_IN_RAO = 1_000_000_000L

/**
 * Builds `SubtensorModule.add_stake_limit(...)`.
 *
 * For root (`netuid == 0`), [spotPriceTaoPerAlpha] is ignored and a
 * cushioned RAO baseline is used. For subnets, pass the live AMM rate
 * (`SubnetTAO / SubnetAlphaIn`); the limit_price is a u64 in RAO per whole
 * alpha (per the pallet docstring) — see [computeLimitPrice].
 */
fun ExtrinsicBuilder.addStakeLimit(
    hotkey: AccountId,
    netuid: Int,
    amount: BigInteger,
    slippage: Double = SubtensorStakingConstants.DEFAULT_SLIPPAGE,
    spotPriceTaoPerAlpha: Double? = null,
): ExtrinsicBuilder = call(
    moduleName = MODULE_NAME,
    callName = "add_stake_limit",
    arguments = mapOf(
        "hotkey" to hotkey,
        "netuid" to netuid.toBigInteger(),
        "amount_staked" to amount,
        "limit_price" to computeLimitPrice(netuid, slippage, isStake = true, spotPriceTaoPerAlpha),
        "allow_partial" to false,
    ),
)

/**
 * Builds `SubtensorModule.remove_stake_limit(...)`. Instant settlement —
 * Bittensor has no unbonding period.
 */
fun ExtrinsicBuilder.removeStakeLimit(
    hotkey: AccountId,
    netuid: Int,
    amount: BigInteger,
    slippage: Double = SubtensorStakingConstants.DEFAULT_SLIPPAGE,
    spotPriceTaoPerAlpha: Double? = null,
): ExtrinsicBuilder = call(
    moduleName = MODULE_NAME,
    callName = "remove_stake_limit",
    arguments = mapOf(
        "hotkey" to hotkey,
        "netuid" to netuid.toBigInteger(),
        "amount_unstaked" to amount,
        "limit_price" to computeLimitPrice(netuid, slippage, isStake = false, spotPriceTaoPerAlpha),
        "allow_partial" to false,
    ),
)

/**
 * Builds `SubtensorModule.move_stake(...)`. Used for "change validator" —
 * single-extrinsic move instead of unstake + restake.
 */
fun ExtrinsicBuilder.moveStake(
    originHotkey: AccountId,
    destinationHotkey: AccountId,
    originNetuid: Int,
    destinationNetuid: Int,
    amount: BigInteger,
): ExtrinsicBuilder = call(
    moduleName = MODULE_NAME,
    callName = "move_stake",
    arguments = mapOf(
        "origin_hotkey" to originHotkey,
        "destination_hotkey" to destinationHotkey,
        "origin_netuid" to originNetuid.toBigInteger(),
        "destination_netuid" to destinationNetuid.toBigInteger(),
        "alpha_amount" to amount,
    ),
)

/**
 * **Root (netuid=0):** alpha == TAO at 1:1. Limit_price is RAO-denominated
 * and cosmetic (no AMM). Stake cushions upward, unstake cushions downward.
 *
 * **Subnets (netuid!=0):** limit_price is u64 RAO per whole alpha (per
 * pallet docstring at add_stake.rs:99). Spot price comes from
 * `SubnetTAO / SubnetAlphaIn` (TAO per alpha); we multiply by ONE_TAO_IN_RAO.
 */
private fun computeLimitPrice(
    netuid: Int,
    slippage: Double,
    isStake: Boolean,
    spotPriceTaoPerAlpha: Double?,
): BigInteger {
    if (netuid == SubtensorStakingConstants.ROOT_NETUID) {
        // Root: no AMM, RAO-denominated 1:1 baseline. Exact BigDecimal rounding
        // (CEILING for stake = max willing to pay, FLOOR for unstake = min willing
        // to accept) to mirror iOS NSDecimalRound exactly. Double ceil/floor drifts
        // by ±1 RAO at some slippages (e.g. 1e9 * 0.995 as a Double floors to
        // 994999999, not 995000000) — see review note 1.
        val multiplier = if (isStake) {
            BigDecimal.ONE.add(BigDecimal.valueOf(slippage))
        } else {
            BigDecimal.ONE.subtract(BigDecimal.valueOf(slippage))
        }
        val cushioned = BigDecimal.valueOf(ONE_TAO_IN_RAO)
            .multiply(multiplier)
            .setScale(0, if (isStake) RoundingMode.CEILING else RoundingMode.FLOOR)
            .toBigInteger()
        return cushioned.max(BigInteger.ONE)
    }

    val spot = spotPriceTaoPerAlpha ?: 0.001
    val adjusted = if (isStake) spot * (1.0 + slippage) else spot * (1.0 - slippage)
    val raoPerAlpha = adjusted * ONE_TAO_IN_RAO.toDouble()
    // Stake rounds up, unstake rounds down — matches root branch so FP
    // drift never tightens the cushion below configured slippage.
    val cushioned = if (isStake) ceil(raoPerAlpha) else floor(raoPerAlpha)
    val rawBits = cushioned.toLong().coerceAtLeast(1L)
    return BigInteger.valueOf(rawBits)
}
