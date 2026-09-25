package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** Slippage is a fraction (0.01 = 1%). Price is rao per one alpha. */
class BittensorSlippage(val fraction: BigDecimal) {

    init {
        require(fraction >= BigDecimal.ZERO && fraction < BigDecimal.ONE) { "Slippage must be in [0, 1)" }
    }

    fun limitPrice(currentAlphaPrice: BigInteger, direction: BittensorSwapDirection): BigInteger {
        require(currentAlphaPrice.signum() > 0 && currentAlphaPrice.bitLength() <= 64) { "Alpha price must be a positive u64" }
        val buying = direction == BittensorSwapDirection.TAO_TO_ALPHA
        val multiplier = if (buying) BigDecimal.ONE + fraction else BigDecimal.ONE - fraction
        val rounding = if (buying) RoundingMode.CEILING else RoundingMode.FLOOR
        val limit = currentAlphaPrice.toBigDecimal().multiply(multiplier).setScale(0, rounding).toBigIntegerExact()

        require(limit.signum() > 0 && limit.bitLength() <= 64) { "Slippage limit must be a positive u64" }
        return limit
    }
}
