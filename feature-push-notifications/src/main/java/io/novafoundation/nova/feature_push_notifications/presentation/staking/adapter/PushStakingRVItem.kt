package io.novafoundation.nova.feature_push_notifications.presentation.staking.adapter

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class PushStakingRVItem(
    val chainId: ChainId,
    val chainName: String,
    val chainIconUrl: String?,
    val isEnabled: Boolean,
)
