package io.novafoundation.nova.feature_staking_api.domain.model

import io.novafoundation.nova.core.model.Network

class StakingAccount(
    val address: String,
    val name: String?,
    val network: Network
)
