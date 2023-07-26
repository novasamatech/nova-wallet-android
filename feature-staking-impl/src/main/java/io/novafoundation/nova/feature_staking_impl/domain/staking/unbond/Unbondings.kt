package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond

import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings.RebondState

class Unbondings(
    val unbondings: List<Unbonding>,
    val anythingToRedeem: Boolean,
    val rebondState: RebondState
) {

    enum class RebondState {
        CAN_REBOND, NOTHING_TO_REBOND, REBOND_NOT_POSSIBLE
    }

    companion object
}

fun Unbondings.Companion.from(unbondings: List<Unbonding>, rebondPossible: Boolean) = Unbondings(
    unbondings = unbondings,
    anythingToRedeem = unbondings.any { it.status is Unbonding.Status.Redeemable },
    rebondState = when {
        !rebondPossible -> RebondState.REBOND_NOT_POSSIBLE
        unbondings.any { it.status is Unbonding.Status.Unbonding } -> RebondState.CAN_REBOND
        else -> RebondState.NOTHING_TO_REBOND
    }
)
