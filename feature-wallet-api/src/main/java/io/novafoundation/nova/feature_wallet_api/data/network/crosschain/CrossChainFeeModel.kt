package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

data class CrossChainFeeModel(
    val paidByOrigin: Balance = BigInteger.ZERO,
    val paidFromHoldingRegister: Balance = BigInteger.ZERO
) {
    companion object
}

fun CrossChainFeeModel.paidByOriginOrNull(): Balance? {
    return if (paidByOrigin.isZero) {
        null
    } else {
        paidByOrigin
    }
}

fun CrossChainFeeModel.Companion.zero() = CrossChainFeeModel()

operator fun CrossChainFeeModel.plus(other: CrossChainFeeModel) = CrossChainFeeModel(
    paidByOrigin = paidByOrigin + other.paidByOrigin,
    paidFromHoldingRegister = paidFromHoldingRegister + other.paidFromHoldingRegister
)

fun CrossChainFeeModel?.orZero() = this ?: CrossChainFeeModel.zero()
