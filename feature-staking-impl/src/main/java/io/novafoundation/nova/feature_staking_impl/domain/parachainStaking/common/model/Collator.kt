package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model

import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot

class Collator(
    val accountIdHex: String,
    val identity: Identity?,
    val snapshot: CollatorSnapshot?,
)
