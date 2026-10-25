package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class BittensorMinimumStakeValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val amount = value.asset.token.planksFromAmount(value.amount)
        val quote = value.latestQuote ?: value.quote
        val serviceFee = quote?.serviceFeeTaoInPlanks ?: BigInteger.ZERO
        val poolFee = quote?.simulation?.taoFee ?: BigInteger.ZERO
        val minimumStake = value.preflight.minimumStake + poolFee
        return validOrError(amount - serviceFee >= minimumStake) {
            BittensorStakeValidationFailure.BelowMinimumStake(bittensorRequiredStakeAmount(minimumStake, quote))
        }
    }
}

/**
 * The smallest entered amount whose stake after the Nova service fee still covers [netMinimum], as on iOS.
 * Root stakes have no quote and no service fee, so the network minimum is shown as is.
 */
internal fun bittensorRequiredStakeAmount(netMinimum: BigInteger, quote: BittensorSubnetBuyQuote?): BigInteger {
    val rate = quote?.serviceFeeRate ?: return netMinimum
    return netMinimum.toBigDecimal()
        .multiply(BigDecimal.ONE + rate)
        .setScale(0, RoundingMode.CEILING)
        .toBigIntegerExact()
}
