package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import java.math.BigInteger

private const val SUBTENSOR_MODULE = "SubtensorModule"
private val ROOT_NETUID = BigInteger.ZERO

fun ExtrinsicBuilder.addBittensorRootStake(
    hotkey: AccountId,
    amount: Balance
) {
    require(amount.isPositive()) { "Stake amount must be positive" }
    require(amount <= MAX_U64) { "Stake amount must fit into u64" }

    call(
        moduleName = SUBTENSOR_MODULE,
        callName = "add_stake",
        arguments = mapOf(
            "hotkey" to hotkey,
            "netuid" to ROOT_NETUID,
            "amount_staked" to amount
        )
    )
}

fun ExtrinsicBuilder.removeBittensorRootStake(
    hotkey: AccountId,
    amount: Balance
) {
    require(hotkey.size == 32 && amount.isPositive() && amount <= MAX_U64)
    call(SUBTENSOR_MODULE, "remove_stake", mapOf("hotkey" to hotkey, "netuid" to ROOT_NETUID, "amount_unstaked" to amount))
}

fun ExtrinsicBuilder.removeFullBittensorStake(
    hotkey: AccountId,
    netuid: BittensorNetuid,
    limitPrice: BigInteger?
) {
    require(hotkey.size == 32)
    require(if (netuid.isRoot()) limitPrice == null else limitPrice != null && limitPrice.isPositive() && limitPrice <= MAX_U64)
    call(
        SUBTENSOR_MODULE,
        "remove_stake_full_limit",
        mapOf("hotkey" to hotkey, "netuid" to BigInteger.valueOf(netuid.value.toLong()), "limit_price" to limitPrice)
    )
}

fun ExtrinsicBuilder.addBittensorSubnetStake(
    subnetId: BittensorNetuid.Subnet,
    hotkey: AccountId,
    taoAmount: Balance,
    limitPrice: BigInteger
) {
    bittensorSubnetStakeCall(
        "add_stake_limit",
        "amount_staked",
        subnetId,
        hotkey,
        taoAmount,
        limitPrice
    )
}

fun ExtrinsicBuilder.removeBittensorSubnetStake(
    subnetId: BittensorNetuid.Subnet,
    hotkey: AccountId,
    alphaAmount: Balance,
    limitPrice: BigInteger
) {
    bittensorSubnetStakeCall(
        "remove_stake_limit",
        "amount_unstaked",
        subnetId,
        hotkey,
        alphaAmount,
        limitPrice
    )
}

private fun ExtrinsicBuilder.bittensorSubnetStakeCall(
    callName: String,
    amountArgument: String,
    subnetId: BittensorNetuid.Subnet,
    hotkey: AccountId,
    amount: Balance,
    limitPrice: BigInteger
) {
    require(hotkey.size == 32) { "Validator hotkey must be AccountId32" }
    require(amount.isPositive() && amount <= MAX_U64) { "Subnet amount must be a positive u64" }
    require(limitPrice.isPositive() && limitPrice <= MAX_U64) { "Subnet limit price must be a positive u64" }

    call(
        moduleName = SUBTENSOR_MODULE,
        callName = callName,
        arguments = mapOf(
            "hotkey" to hotkey,
            "netuid" to BigInteger.valueOf(subnetId.value.toLong()),
            amountArgument to amount,
            "limit_price" to limitPrice,
            "allow_partial" to false
        )
    )
}

private val MAX_U64 = BigInteger.ONE.shiftLeft(64) - BigInteger.ONE
