package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigInteger

private val PRICE_SCALE = BigInteger.TEN.pow(BITTENSOR_ALPHA_DECIMALS)

private val PRICE_IMPACT_WARNING_DENOMINATOR = BigInteger.valueOf(100)

/** Match iOS: round the average buy price and the resulting post-trade price upwards. */
fun BittensorSubnetBuyQuote.postTradePrice(): BigInteger {
    val averagePrice = (simulation.amountIn * PRICE_SCALE).divideRoundingUp(alphaOutInPlanks)
    return (averagePrice * averagePrice).divideRoundingUp(spotPrice)
}

fun BittensorSubnetBuyQuote.hasHighPriceImpact(): Boolean {
    val priceDifference = (postTradePrice() - spotPrice).abs()
    return priceDifference * PRICE_IMPACT_WARNING_DENOMINATOR > spotPrice
}

private fun BigInteger.divideRoundingUp(divisor: BigInteger): BigInteger = (this + divisor - BigInteger.ONE) / divisor
