package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload

class BittensorExitFeeRemainderValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        val remainder = value.preflight.stake - value.quote.amount - value.fee
        return validOrError(
            !value.feeFromStake || value.quote.fullExit || remainder.isZero || value.preflight.isOwnHotkey ||
                remainder >= value.preflight.minNominatorStake
        ) { BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM }
    }
}
