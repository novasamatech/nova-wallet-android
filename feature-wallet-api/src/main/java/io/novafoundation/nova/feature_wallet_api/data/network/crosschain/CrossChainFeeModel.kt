package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

data class CrossChainFeeModel(
    val deliveryFees: Balance = BigInteger.ZERO,
    val executionFees: Balance = BigInteger.ZERO
) {
    companion object
}

fun CrossChainFeeModel.deliveryFeesOrNull(): Balance? {
    return if (deliveryFees.isZero) {
        null
    } else {
        deliveryFees
    }
}

fun CrossChainFeeModel.Companion.zero() = CrossChainFeeModel()

operator fun CrossChainFeeModel.plus(other: CrossChainFeeModel) = CrossChainFeeModel(
    deliveryFees = deliveryFees + other.deliveryFees,
    executionFees = executionFees + other.executionFees
)

fun CrossChainFeeModel?.orZero() = this ?: CrossChainFeeModel.zero()
