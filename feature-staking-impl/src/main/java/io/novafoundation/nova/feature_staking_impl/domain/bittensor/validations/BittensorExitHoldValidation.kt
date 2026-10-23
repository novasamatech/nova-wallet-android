package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

/** Blocks left on the root hold that refuses the withdrawal; iOS checks the plan's validators in order and names the first held one. */
fun BittensorExitValidationPayload.rootHoldBlocks(): BigInteger {
    val holds = quote.plan?.slices?.mapNotNull { preflight.groupMembers[it.hotkey]?.holdRemaining }
    return holds?.firstOrNull { it.isPositive() } ?: holds?.maxOrNull()
        ?: if (quote.fullExit) preflight.groupRootHoldRemaining else preflight.rootHoldRemaining
}

class BittensorExitHoldValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        return validOrError(
            value.rootHoldBlocks().isZero
        ) { BittensorExitValidationFailure.ROOT_LOCKED }
    }
}
