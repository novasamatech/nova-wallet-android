package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import java.math.BigInteger

class BittensorExitPlanValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload) = value.quote.plan?.let { plan ->
        // Legacy grouped partial payloads without a basis must return to amount entry;
        // deriving their identity from today's group would silently accept a changed group.
        val singlePartial = plan.slices.size == 1 && !plan.slices.single().fullExit
        val sameGroup = singlePartial || value.quote.fullExit ||
            value.quote.groupHotkeys.isNotEmpty() && value.quote.groupHotkeys == value.preflight.groupStakes.keys
        validOrError(
            sameGroup && plan.slices.all { slice ->
                val stake = value.preflight.groupStakes[slice.hotkey]
                val member = value.preflight.groupMembers[slice.hotkey]
                stake != null && member != null && if (slice.fullExit) {
                    (stake - slice.amount).abs() * BigInteger.valueOf(100) <= slice.amount && member.free == stake
                } else {
                    slice.amount <= member.free
                }
            }
        ) { BittensorExitValidationFailure.FULL_POSITION_CHANGED }
    } ?: valid()
}
