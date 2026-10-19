package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot

class BittensorExitRemainderValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        val quote = value.quote
        val partials = quote.plan?.slices?.filterNot { it.fullExit }
        if (partials != null) {
            val dust = partials.any { slice ->
                val member = value.preflight.groupMembers[slice.hotkey] ?: return@any false
                val remainder = member.stake - slice.amount
                val tao = if (quote.netuid.isRoot()) remainder else remainder * requireNotNull(quote.limitPrice) / BittensorProtocolUnits.RAO_PER_TAO
                !member.isOwnHotkey && !remainder.isZero && tao < value.preflight.minNominatorStake
            }
            if (!dust) return valid()
            return if (value.preflight.canExitAll) {
                validOrWarning(false) { BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL }
            } else {
                validOrError(false) { BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM }
            }
        }
        val remainder = value.preflight.stake - quote.amount
        val remainderTao = if (quote.netuid.isRoot()) {
            remainder
        } else {
            remainder * requireNotNull(quote.limitPrice) / BittensorProtocolUnits.RAO_PER_TAO
        }
        if (quote.fullExit || remainder.isZero || value.preflight.isOwnHotkey || remainderTao >= value.preflight.minNominatorStake) return valid()
        return if (value.preflight.canExitAll) {
            validOrWarning(false) { BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL }
        } else {
            validOrError(false) { BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM }
        }
    }
}
