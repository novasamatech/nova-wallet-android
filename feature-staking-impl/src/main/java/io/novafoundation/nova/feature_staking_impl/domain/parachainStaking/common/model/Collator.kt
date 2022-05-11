package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model

import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import java.math.BigInteger

class Collator(
    val accountIdHex: String,
    val identity: Identity?,
    val snapshot: CollatorSnapshot?,
    val minimumStake: BigInteger,
)


fun CollatorSnapshot.minimumStake(systemForcedMinStake: BigInteger): BigInteger {
    return delegations.minOfOrNull { it.balance } ?: systemForcedMinStake
}
