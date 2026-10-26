package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload

class BittensorExitBalanceValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        val stake = if (value.quote.fullExit || value.quote.plan != null) value.preflight.totalStake else value.preflight.stake
        val amount = if (value.quote.fullExit) stake else value.quote.amount
        return validOrError(
            amount <= stake && amount <= value.preflight.availability.available && value.quote.plan?.slices.orEmpty().all { slice ->
                val member = value.preflight.groupMembers[slice.hotkey]
                member == null || (if (slice.fullExit) member.stake == member.free else slice.amount <= member.free)
            }
        ) { BittensorExitValidationFailure.INSUFFICIENT_STAKE }
    }
}
