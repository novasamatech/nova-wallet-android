package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.math.BigInteger

data class BittensorExitAvailability(val total: BigInteger, val locked: BigInteger, val available: BigInteger)

data class BittensorExitPreflight(
    val stake: BigInteger,
    val availability: BittensorExitAvailability,
    val rootHoldRemaining: BigInteger,
    val rootHoldEnabled: Boolean,
    val minStake: BigInteger,
    val minNominatorStake: BigInteger,
    val isOwnHotkey: Boolean
)
