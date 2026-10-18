package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload

class BittensorExitFeeRemainderValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        if (!value.feeFromStake) return valid()
        if (value.quote.fullExit) return valid()
        if (value.preflight.isOwnHotkey) return valid()

        val remainingStakeAfterFee = value.preflight.stake - value.quote.amount - value.fee
        if (remainingStakeAfterFee.isZero) return valid()

        return validOrError(remainingStakeAfterFee >= value.preflight.minNominatorStake) {
            BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM
        }
    }
}
