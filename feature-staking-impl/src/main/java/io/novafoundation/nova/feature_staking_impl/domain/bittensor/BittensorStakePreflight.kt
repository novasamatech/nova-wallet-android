package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigInteger

data class BittensorStakePreflight(
    val hotkeyRegistered: Boolean,
    val subnetExists: Boolean,
    val stakingEnabled: Boolean,
    val coldKeySwapPending: Boolean,
    val safeModeActive: Boolean,
    val minimumStake: BigInteger,
    val currentBlock: BigInteger,
    val rootUnlockInterval: BigInteger,
    val lastStakeBlock: BigInteger?
)
