package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

private val PRICE_SCALE = BigInteger.TEN.pow(BITTENSOR_ALPHA_DECIMALS)

private val PRICE_IMPACT_WARNING_DENOMINATOR = BigInteger.valueOf(100)

private const val PRICE_IMPACT_SCALE = 6

/** The move a trade causes in the pool price away from [spotPrice]. */
class BittensorPriceImpact(private val spotPrice: BigInteger, postTradePrice: BigInteger) {

    private val priceMove = (postTradePrice - spotPrice).abs()

    /** A move of more than 1 % of the spot price. */
    val isHigh: Boolean
        get() = priceMove * PRICE_IMPACT_WARNING_DENOMINATOR > spotPrice

    val fraction: BigDecimal
        get() = priceMove.toBigDecimal().divide(spotPrice.toBigDecimal(), PRICE_IMPACT_SCALE, RoundingMode.DOWN)
}

/** Match iOS: round the average buy price and the resulting post-trade price upwards. */
fun BittensorSubnetBuyQuote.postTradePrice(): BigInteger {
    val averagePrice = (simulation.amountIn * PRICE_SCALE).divideRoundingUp(alphaOutInPlanks)
    return (averagePrice * averagePrice).divideRoundingUp(spotPrice)
}

fun BittensorSubnetBuyQuote.priceImpact() = BittensorPriceImpact(spotPrice, postTradePrice())

private fun BigInteger.divideRoundingUp(divisor: BigInteger): BigInteger = (this + divisor - BigInteger.ONE) / divisor
