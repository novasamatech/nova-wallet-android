package io.novafoundation.nova.feature_staking_impl.data.subtensor.extrinsic

import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import java.math.BigInteger
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
 * (`SubnetTAO / SubnetAlphaIn`); the limit_price is encoded as I96F32.
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
        val multiplier = if (isStake) 1.0 + slippage else 1.0 - slippage
        val raw = ONE_TAO_IN_RAO.toDouble() * multiplier
        // Stake rounds up (max price willing to pay), unstake rounds down (min
        // price willing to accept). iOS uses NSDecimalRound(.up/.down); plain
        // toLong() would truncate toward zero on both directions, tightening
        // the cushion for stake — see review note 1.
        val cushioned = if (isStake) ceil(raw).toLong() else floor(raw).toLong()
        return BigInteger.valueOf(cushioned.coerceAtLeast(1L))
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
