package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import java.math.BigInteger

/** TAO value of the first remainder the network would sweep, and the nominator minimum it falls below. */
data class BittensorExitDustRemainder(val remainderTao: BigInteger, val minimumTao: BigInteger)

/**
 * Subtensor sweeps a non-owner remainder whose TAO value (alpha × price for subnets) is below the nominator
 * minimum. Valued at the protective limit price, as iOS `SubtensorAmountPolicy.sellPlan` does.
 */
fun BittensorExitValidationPayload.dustRemainder(): BittensorExitDustRemainder? {
    val minimum = preflight.minNominatorStake
    val taoOf = { alpha: BigInteger ->
        if (quote.netuid.isRoot()) alpha else alpha * requireNotNull(quote.limitPrice) / BittensorProtocolUnits.RAO_PER_TAO
    }
    val partials = quote.plan?.slices?.filterNot { it.fullExit }
    if (partials != null) {
        return partials.firstNotNullOfOrNull { slice ->
            val member = preflight.groupMembers[slice.hotkey] ?: return@firstNotNullOfOrNull null
            val remainder = member.stake - slice.amount
            if (member.isOwnHotkey || remainder.signum() <= 0) return@firstNotNullOfOrNull null
            taoOf(remainder).takeIf { it < minimum }?.let { BittensorExitDustRemainder(it, minimum) }
        }
    }
    if (quote.fullExit || preflight.isOwnHotkey) return null
    val remainder = preflight.stake - quote.amount
    if (remainder.signum() <= 0) return null
    return taoOf(remainder).takeIf { it < minimum }?.let { BittensorExitDustRemainder(it, minimum) }
}

class BittensorExitRemainderValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        value.dustRemainder() ?: return valid()
        return when {
            // A free remainder is returned to the transferable balance: warn, let the user proceed or exit all.
            value.preflight.canExitAll -> validOrWarning(value.remainderSweepAccepted) { BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL }
            // A locked remainder would be erased without payout.
            else -> validationError(BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM)
        }
    }
}
