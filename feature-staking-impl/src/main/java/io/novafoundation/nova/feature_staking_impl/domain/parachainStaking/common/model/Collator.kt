package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import java.math.BigDecimal
import java.math.BigInteger

class Collator(
    val accountIdHex: String,
    val address: String,
    val identity: Identity?,
    val snapshot: CollatorSnapshot?,
    val minimumStakeToGetRewards: BigInteger?,
    val apr: BigDecimal?,
) : Identifiable {

    override val identifier: String = accountIdHex
}

val Collator.isElected
    get() = snapshot != null

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
