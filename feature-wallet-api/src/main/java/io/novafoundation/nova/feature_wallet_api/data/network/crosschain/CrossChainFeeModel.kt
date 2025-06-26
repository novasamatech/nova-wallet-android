package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

data class CrossChainFeeModel(
    val paidByAccount: Balance = BigInteger.ZERO,
    val paidFromHolding: Balance = BigInteger.ZERO
) {
    companion object
}

fun CrossChainFeeModel.paidByAccountOrNull(): Balance? {
    return paidByAccount.takeIf { paidByAccount.isPositive() }
}

fun CrossChainFeeModel.Companion.zero() = CrossChainFeeModel()

operator fun CrossChainFeeModel.plus(other: CrossChainFeeModel) = CrossChainFeeModel(
    paidByAccount = paidByAccount + other.paidByAccount,
    paidFromHolding = paidFromHolding + other.paidFromHolding
)

fun CrossChainFeeModel?.orZero() = this ?: CrossChainFeeModel.zero()
