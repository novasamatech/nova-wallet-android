package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.duration

import kotlin.time.Duration

data class UnlockingDuration(
    val validator: Duration,
    val nominator: Duration
) {

    fun valueFor(variant: UnstakingDurationVariant): Duration = when (variant) {
        UnstakingDurationVariant.FULL -> validator
        UnstakingDurationVariant.NOMINATOR -> nominator
    }
}
