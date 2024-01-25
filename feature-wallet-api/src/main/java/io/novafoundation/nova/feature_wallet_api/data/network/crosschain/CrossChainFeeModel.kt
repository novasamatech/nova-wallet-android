package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

data class CrossChainFeeModel(
    val senderPart: Balance = BigInteger.ZERO,
    val holdingPart: Balance = BigInteger.ZERO
) {
    companion object
}

fun CrossChainFeeModel.Companion.zero() = CrossChainFeeModel()

operator fun CrossChainFeeModel.plus(other: CrossChainFeeModel) = CrossChainFeeModel(
    senderPart = senderPart + other.senderPart,
    holdingPart = holdingPart + other.holdingPart
)

fun CrossChainFeeModel?.orZero() = if (this == null) {
    CrossChainFeeModel.zero()
} else {
    this
}
