package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal

/** Informational compound projection. annualPercent is already net of validator take. */
internal fun bittensorMonthlyProjection(principal: BigDecimal, annualPercent: BigDecimal): BigDecimal {
    require(principal.signum() >= 0 && annualPercent > BigDecimal("-100"))
    val annualMultiplier = BigDecimal.ONE + annualPercent.movePointLeft(2)
    val monthlyRate = Math.pow(annualMultiplier.toDouble(), 1.0 / 12.0) - 1.0
    require(!monthlyRate.isNaN() && !monthlyRate.isInfinite())
    return principal.multiply(BigDecimal.valueOf(monthlyRate))
}
