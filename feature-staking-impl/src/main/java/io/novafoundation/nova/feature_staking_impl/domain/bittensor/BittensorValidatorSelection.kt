package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal
import java.math.BigInteger

data class BittensorValidatorGates(val maxTake: BigDecimal, val requirePermit: Boolean, val requireActive: Boolean) {

    init {
        require(maxTake >= BigDecimal.ZERO && maxTake <= BigDecimal.ONE)
    }

    fun allowsTake(take: BigInteger): Boolean {
        require(take >= BigInteger.ZERO && take <= BigInteger.valueOf(65535))
        // Compare the integers exactly; rounding the displayed percentage must not affect eligibility.
        return take.toBigDecimal() <= maxTake * BigDecimal(65535)
    }
}

data class BittensorSubnetValidator(val validator: BittensorValidator, val address: String, val take: BigDecimal, val selectable: Boolean)
