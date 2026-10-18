package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

// Subtensor's delegate take is a reward commission encoded as a fraction of the full u16 range.
private val VALIDATOR_COMMISSION_DENOMINATOR = UShort.MAX_VALUE.toInt().toBigInteger()

private const val COMMISSION_RATE_SCALE = 12

data class BittensorValidatorGates(
    val maxCommissionRate: BigDecimal,
    val requirePermit: Boolean,
    val requireActive: Boolean
) {

    init {
        require(maxCommissionRate.isNonNegative && maxCommissionRate <= BigDecimal.ONE)
    }

    fun allowsCommission(commissionInU16: BigInteger): Boolean {
        require(commissionInU16.isNonNegative && commissionInU16 <= VALIDATOR_COMMISSION_DENOMINATOR)
        // Compare the integers exactly; rounding the displayed percentage must not affect eligibility.
        return commissionInU16.toBigDecimal() <= maxCommissionRate * VALIDATOR_COMMISSION_DENOMINATOR.toBigDecimal()
    }
}

internal fun validatorCommissionRateFromU16(commissionInU16: BigInteger): BigDecimal = commissionInU16.toBigDecimal()
    .divide(VALIDATOR_COMMISSION_DENOMINATOR.toBigDecimal(), COMMISSION_RATE_SCALE, RoundingMode.HALF_EVEN)

data class BittensorSubnetValidator(
    val validator: BittensorValidator,
    val address: String,
    val commissionRate: BigDecimal,
    val selectable: Boolean,
    val recommended: Boolean = false
)
