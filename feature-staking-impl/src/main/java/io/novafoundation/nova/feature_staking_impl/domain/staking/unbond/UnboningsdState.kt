package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond

import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding

class UnboningsdState(
    val unbondings: List<Unbonding>,
    val anythingToRedeem: Boolean,
    val anythingToUnbond: Boolean
)
