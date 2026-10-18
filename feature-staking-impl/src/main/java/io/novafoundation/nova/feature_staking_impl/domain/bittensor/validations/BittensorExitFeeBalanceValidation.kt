package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot

class BittensorExitFeeBalanceValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        return validOrError(
            !value.feeFromStake || value.quote.netuid.isRoot() && value.supportsFeeFromStake &&
                value.quote.amount > value.fee + value.existentialDeposit &&
                (value.quote.fullExit || value.quote.amount + value.fee <= value.preflight.stake)
        ) { BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE }
    }
}
