package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items

import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.custom.ConnectionStateModel

data class NetworkNodeRvItem(
    val id: String,
    val name: String,
    val socketAddress: String,
    val isEditable: Boolean,
    val isSelected: Boolean,
    val connectionState: ConnectionStateModel
) : NetworkConnectionRvItem
