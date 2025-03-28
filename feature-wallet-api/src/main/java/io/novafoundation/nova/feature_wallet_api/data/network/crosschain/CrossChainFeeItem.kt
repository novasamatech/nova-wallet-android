package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

data class CrossChainFeeItem(
    val paidByAccount: Balance = BigInteger.ZERO,
    val paidFromHolding: Balance = BigInteger.ZERO
) {
    companion object
}

fun CrossChainFeeItem.paidByAccountOrNull(): Balance? {
    return paidByAccount.takeIf { paidByAccount.isPositive() }
}

fun CrossChainFeeItem.Companion.zero() = CrossChainFeeItem()

operator fun CrossChainFeeItem.plus(other: CrossChainFeeItem) = CrossChainFeeItem(
    paidByAccount = paidByAccount + other.paidByAccount,
    paidFromHolding = paidFromHolding + other.paidFromHolding
)

fun CrossChainFeeItem?.orZero() = this ?: CrossChainFeeItem.zero()
