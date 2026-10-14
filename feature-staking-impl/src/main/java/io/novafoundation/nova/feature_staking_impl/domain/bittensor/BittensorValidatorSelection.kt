package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import java.math.BigDecimal
import java.math.BigInteger

data class BittensorValidatorGates(val maxTake: BigDecimal, val requirePermit: Boolean, val requireActive: Boolean) {

    init {
        require(maxTake.isNonNegative && maxTake <= BigDecimal.ONE)
    }

    fun allowsTake(take: BigInteger): Boolean {
        require(take.isNonNegative && take <= BigInteger.valueOf(65535))
        // Compare the integers exactly; rounding the displayed percentage must not affect eligibility.
        return take.toBigDecimal() <= maxTake * BigDecimal(65535)
    }
}

data class BittensorSubnetValidator(val validator: BittensorValidator, val address: String, val take: BigDecimal, val selectable: Boolean)
