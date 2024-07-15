package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.common.ConnectionStateModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class NetworkListRvItem(
    val chainIcon: Icon,
    val chainId: ChainId,
    val title: String,
    val subtitle: String?,
    val chainLabel: String?,
    val disabled: Boolean,
    val status: ConnectionStateModel?
)
