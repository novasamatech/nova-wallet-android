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

/**
 * TAO value of a remainder the network would sweep and the nominator minimum it falls below. A [returned] remainder goes
 * to the transferable balance; otherwise the network erases it without payout.
 */
data class BittensorExitDustRemainder(val remainderTao: BigInteger, val minimumTao: BigInteger, val returned: Boolean)

/**
 * Subtensor sweeps a non-owner remainder whose TAO value (alpha × price for subnets) is below the nominator
 * minimum, checked after the sale at the pool price it leaves (`clear_small_nomination_if_required`); the limit price
 * bounds that price when the quote carries no simulation. As iOS `SubtensorAmountPolicy.sellPlan` decides per slice,
 * the sweep pays out only when the coldkey's availability on the netuid covers that validator's position.
 */
fun BittensorExitValidationPayload.dustRemainders(): List<BittensorExitDustRemainder> {
    val minimum = preflight.minNominatorStake
    val price = (latestQuote ?: quote).postTradePrice ?: quote.limitPrice
    val dustOf = { position: BigInteger, amount: BigInteger ->
        val remainder = position - amount
        val value = if (quote.netuid.isRoot()) remainder else remainder * requireNotNull(price) / BittensorProtocolUnits.RAO_PER_TAO
        BittensorExitDustRemainder(value, minimum, returned = preflight.availability.available >= position)
            .takeIf { remainder.signum() > 0 && value < minimum }
    }
    val partials = quote.plan?.slices?.filterNot { it.fullExit }
    if (partials != null) {
        return partials.mapNotNull { slice ->
            val member = preflight.groupMembers[slice.hotkey] ?: return@mapNotNull null
            if (member.isOwnHotkey) null else dustOf(member.stake, slice.amount)
        }
    }
    if (quote.fullExit || preflight.isOwnHotkey) return emptyList()
    return listOfNotNull(dustOf(preflight.stake, quote.amount))
}

/** The remainder validation stops at: slices are checked in plan order and an accepted sweep lets the next one through. */
fun BittensorExitValidationPayload.dustRemainder(): BittensorExitDustRemainder? {
    return dustRemainders().firstOrNull { !it.returned || !remainderSweepAccepted }
}

class BittensorExitRemainderValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        val dust = value.dustRemainder() ?: return valid()
        return when {
            // A returned remainder closes the position: warn, let the user proceed.
            dust.returned -> validOrWarning(value.remainderSweepAccepted) { BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL }
            // A locked remainder would be erased without payout.
            else -> validationError(BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM)
        }
    }
}
