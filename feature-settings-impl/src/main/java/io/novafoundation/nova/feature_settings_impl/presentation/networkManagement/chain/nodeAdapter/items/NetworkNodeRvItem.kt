package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items

import androidx.annotation.ColorRes
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.common.ConnectionStateModel

data class NetworkNodeRvItem(
    val id: String,
    val name: String,
    @ColorRes val nameColorRes: Int,
    val socketAddress: String,
    val isEditable: Boolean,
    val isSelected: Boolean,
    val connectionState: ConnectionStateModel,
    val isSelectable: Boolean
) : NetworkConnectionRvItem
