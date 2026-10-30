package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.common.validation.valid
import java.math.BigInteger
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload

class BittensorFullExitValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        if (!value.quote.fullExit) return valid()

        val reviewedHotkeys = value.quote.exitHotkeys
        val liveHotkeys = value.preflight.orderedHotkeys
        val sameValidators = if (reviewedHotkeys.isEmpty()) liveHotkeys.isEmpty() else reviewedHotkeys == liveHotkeys
        val reviewedAmount = value.quote.amount
        val amountDrift = (value.preflight.totalStake - reviewedAmount).abs()
        val withinTolerance = amountDrift * PERCENT_DENOMINATOR <= reviewedAmount
        return validOrError(
            sameValidators && withinTolerance
        ) { BittensorExitValidationFailure.FULL_POSITION_CHANGED }
    }

    private companion object {

        val PERCENT_DENOMINATOR = BigInteger.valueOf(100)
    }
}
