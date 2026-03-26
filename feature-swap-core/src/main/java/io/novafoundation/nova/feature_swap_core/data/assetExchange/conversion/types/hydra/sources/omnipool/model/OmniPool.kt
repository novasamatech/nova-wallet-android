package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.hydra_dx_math.HydraDxMathConversions.fromBridgeResultToBalance
import io.novafoundation.nova.hydra_dx_math.omnipool.OmniPoolMathBridge
import java.math.BigInteger

class OmniPool(
    val tokens: Map<HydraDxAssetId, OmniPoolToken>,
    val maxSlipFee: Fraction
)

class OmniPoolFees(
    val protocolFee: Fraction,
    val assetFee: Fraction
)

class OmniPoolToken(
    val hubReserve: BigInteger,
    val shares: BigInteger,
    val protocolShares: BigInteger,
    val tradeability: Tradeability,
    val balance: BigInteger,
    val fees: OmniPoolFees
)

fun OmniPool.quote(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amount: BigInteger,
    direction: SwapDirection
): BigInteger? {
    return when (direction) {
        SwapDirection.SPECIFIED_IN -> calculateOutGivenIn(assetIdIn, assetIdOut, amount)
        SwapDirection.SPECIFIED_OUT -> calculateInGivenOut(assetIdIn, assetIdOut, amount)
    }
}

fun OmniPool.calculateOutGivenIn(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amountIn: BigInteger
): BigInteger? {
    val tokenInState = tokens.getValue(assetIdIn)
    val tokenOutState = tokens.getValue(assetIdOut)

    return OmniPoolMathBridge.calculate_out_given_in(
        tokenInState.balance.toString(),
        tokenInState.hubReserve.toString(),
        tokenInState.shares.toString(),
        tokenOutState.balance.toString(),
        tokenOutState.hubReserve.toString(),
        tokenOutState.shares.toString(),
        amountIn.toString(),
        tokenOutState.fees.assetFee.inFraction.toBigDecimal().toPlainString(),
        tokenInState.fees.protocolFee.inFraction.toBigDecimal().toPlainString(),
        maxSlipFee.inFraction.toBigDecimal().toPlainString()
    ).fromBridgeResultToBalance()
}

fun OmniPool.calculateInGivenOut(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amountOut: BigInteger
): BigInteger? {
    val tokenInState = tokens.getValue(assetIdIn)
    val tokenOutState = tokens.getValue(assetIdOut)

    return OmniPoolMathBridge.calculate_in_given_out(
        tokenInState.balance.toString(),
        tokenInState.hubReserve.toString(),
        tokenInState.shares.toString(),
        tokenOutState.balance.toString(),
        tokenOutState.hubReserve.toString(),
        tokenOutState.shares.toString(),
        amountOut.toString(),
        tokenOutState.fees.assetFee.inFraction.toBigDecimal().toPlainString(),
        tokenInState.fees.protocolFee.inFraction.toBigDecimal().toPlainString(),
        maxSlipFee.inFraction.toBigDecimal().toPlainString()
    ).fromBridgeResultToBalance()
}
