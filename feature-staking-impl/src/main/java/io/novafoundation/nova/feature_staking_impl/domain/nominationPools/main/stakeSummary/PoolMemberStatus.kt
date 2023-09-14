package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary

import kotlin.time.Duration

sealed class PoolMemberStatus {

    object Active : PoolMemberStatus()

    class Waiting(val timeLeft: Duration) : PoolMemberStatus()

    object Inactive : PoolMemberStatus()
}
