package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger
import kotlin.math.floor

typealias OmniPoolTokenId = BigInteger

class OmniPool(
    val tokens: Map<OmniPoolTokenId, OmniPoolToken>,
)

class OmniPoolToken(
    val hubReserve: Balance,
    val shares: Balance,
    val protocolShares: Balance,
    val tradeability: Tradeability,
    val balance: Balance
)

fun OmniPool.quote(
    assetIdIn: OmniPoolTokenId,
    assetIdOut: OmniPoolTokenId,
    amount: Balance,
    direction: SwapDirection
): Balance {
    return when(direction) {
        SwapDirection.SPECIFIED_IN -> calculateOutGivenIn(assetIdIn, assetIdOut, amount)
        SwapDirection.SPECIFIED_OUT -> calculateInGivenOut(assetIdIn, assetIdOut, amount)
    }
}

fun OmniPool.calculateOutGivenIn(
    assetIdIn: OmniPoolTokenId,
    assetIdOut: OmniPoolTokenId,
    amountIn: Balance
): Balance {
    val tokenInState = tokens.getValue(assetIdIn)
    val tokenOutState = tokens.getValue(assetIdOut)

    val inHubReserve = tokenInState.hubReserve.toDouble()
    val inReserve = tokenInState.balance.toDouble()

    val inAmount = amountIn.toDouble()

    // TODO take fees into account
    val protocolFee = Perbill.zero()
    val assetFee =  Perbill.zero()

    val deltaHubReserveIn = inAmount * inHubReserve / (inReserve + inAmount)

    val protocolFeeAmount = floor(protocolFee.value * deltaHubReserveIn)

    val deltaHubReserveOut = deltaHubReserveIn - protocolFeeAmount

    val outReserveHp = tokenOutState.balance.toDouble()
    val outHubReserveHp = tokenOutState.hubReserve.toDouble()

    val deltaReserveOut = outReserveHp * deltaHubReserveOut / (outHubReserveHp + deltaHubReserveOut)
    val amountOut = deltaReserveOut.deductFraction(assetFee)

    return amountOut.toBigDecimal().toBigInteger()
}

fun OmniPool.calculateInGivenOut(
    assetIdIn: OmniPoolTokenId,
    assetIdOut: OmniPoolTokenId,
    amountOut: Balance
): Balance {
    // TODO
    return BigInteger.ZERO
}


private fun Double.deductFraction(perbill: Perbill): Double = this - this * perbill.value

//
//private val INTEGER_FLOOR_MATH_CONTEXT = MathContext(0, RoundingMode.FLOOR)
//
//private fun BigDecimal.floorToInteger() = round(INTEGER_FLOOR_MATH_CONTEXT)
