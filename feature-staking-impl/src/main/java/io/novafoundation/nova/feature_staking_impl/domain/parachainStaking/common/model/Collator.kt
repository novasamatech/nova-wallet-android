package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model

import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import java.math.BigInteger

class Collator(
    val accountIdHex: String,
    val identity: Identity?,
    val snapshot: CollatorSnapshot,
    val minimumStakeToGetRewards: BigInteger,
)

fun CollatorSnapshot.minimumStake(
    systemForcedMinStake: BigInteger,
    maxRewardableDelegatorsPerCollator: BigInteger
): BigInteger {
    if (delegations.size < maxRewardableDelegatorsPerCollator.toInt()) {
        return systemForcedMinStake
    }

    val minStakeToGetRewards = delegations.minOfOrNull { it.balance } ?: systemForcedMinStake

    return minStakeToGetRewards.coerceAtLeast(systemForcedMinStake)
}
