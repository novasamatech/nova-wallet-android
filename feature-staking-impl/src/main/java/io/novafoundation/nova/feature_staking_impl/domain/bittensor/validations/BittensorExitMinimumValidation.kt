package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import java.math.BigInteger

class BittensorExitMinimumValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        val quote = value.quote
        val partials = quote.plan?.slices?.filterNot { it.fullExit }
        val fee = if (quote.netuid.isRoot() && value.transferable < value.fee + value.existentialDeposit) value.fee else BigInteger.ZERO
        val sufficient = partials?.all { slice ->
            val minimum = if (quote.netuid.isRoot()) {
                slice.amount - fee
            } else {
                slice.amount * quote.minimumTaoOut / quote.amount
            }
            minimum >= value.preflight.minStake
        } ?: (quote.fullExit || quote.minimumTaoOut - fee >= value.preflight.minStake)
        return validOrError(sufficient) { BittensorExitValidationFailure.BELOW_MINIMUM }
    }
}
