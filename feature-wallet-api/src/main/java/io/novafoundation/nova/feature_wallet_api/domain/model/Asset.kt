package io.novafoundation.nova.feature_wallet_api.domain.model

import java.math.BigInteger

class Asset(
    val token: Token,

    // Non-reserved part of the balance. There may still be restrictions on
    // this, but it is the total pool what may in principle be transferred,
    // reserved.
    val freeInPlanks: BigInteger,

    // Balance which is reserved and may not be used at all.
    // This balance is a 'reserve' balance that different subsystems use in
    // order to set aside tokens that are still 'owned' by the account
    // holder, but which are suspendable
    val reservedInPlanks: BigInteger,

    // / The amount that `free` may not drop below when withdrawing.
    val frozenInPlanks: BigInteger,

    // TODO move to runtime storage
    val bondedInPlanks: BigInteger,
    val redeemableInPlanks: BigInteger,
    val unbondingInPlanks: BigInteger
) {

    // Non-reserved plus reserved
    val totalInPlanks = freeInPlanks + reservedInPlanks
    // Free without its min threshold, represented by frozen
    val transferableInPlanks = freeInPlanks - frozenInPlanks

    // balance that cannot be used for transfers (non-transferable) for any reason
    val lockedInPlanks = totalInPlanks - transferableInPlanks

    // TODO maybe move to extension fields?
    //  Check affect on performance, if those fields will be recalculated on each usage
    val total = token.amountFromPlanks(totalInPlanks)
    val reserved = token.amountFromPlanks(reservedInPlanks)
    val locked = token.amountFromPlanks(lockedInPlanks)
    val transferable = token.amountFromPlanks(transferableInPlanks)

    // TODO move to runtime storage
    val bonded = token.amountFromPlanks(bondedInPlanks)
    val redeemable = token.amountFromPlanks(redeemableInPlanks)
    val unbonding = token.amountFromPlanks(unbondingInPlanks)

    val dollarAmount = token.priceOf(total)
}
