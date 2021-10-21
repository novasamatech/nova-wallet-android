package io.novafoundation.nova.feature_account_impl.presentation.node.model

import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.network.model.NetworkModel

data class NodeModel(
    val id: Int,
    val name: String,
    val link: String,
    val networkModelType: NetworkModel.NetworkTypeUI,
    val isDefault: Boolean,
    val isActive: Boolean
)
