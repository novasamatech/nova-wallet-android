package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.stakeSummary

import kotlin.time.Duration

sealed class DelegatorStatus {

    object Active : DelegatorStatus()

    class Waiting(val timeLeft: Duration) : DelegatorStatus()

    object Inactive : DelegatorStatus()
}
