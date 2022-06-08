package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond

import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding

class Unbondings(
    val unbondings: List<Unbonding>,
    val anythingToRedeem: Boolean,
    val anythingToUnbond: Boolean
) {

    companion object
}

fun Unbondings.Companion.from(unbondings: List<Unbonding>) = Unbondings(
    unbondings = unbondings,
    anythingToRedeem = unbondings.any { it.status is Unbonding.Status.Redeemable },
    anythingToUnbond = unbondings.any { it.status is Unbonding.Status.Unbonding }
)
