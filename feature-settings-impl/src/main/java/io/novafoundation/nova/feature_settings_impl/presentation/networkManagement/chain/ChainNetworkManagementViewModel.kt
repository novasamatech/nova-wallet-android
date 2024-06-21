package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.ChainNetworkState
import io.novafoundation.nova.feature_settings_impl.domain.NetworkManagementChainInteractor
import io.novafoundation.nova.feature_settings_impl.domain.NodeHealthState
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkConnectionRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkNodeRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkNodesAddCustomRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.custom.ConnectionStateModel
import io.novafoundation.nova.runtime.ext.isEnabled
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChainNetworkManagementViewModel(
    private val router: SettingsRouter,
    private val resourceManager: ResourceManager,
    private val networkManagementChainInteractor: NetworkManagementChainInteractor,
    private val payload: ChainNetworkManagementPayload
) : BaseViewModel() {

    private val chainNetworkStateFlow = networkManagementChainInteractor.chainStateFlow(payload.chainId)
        .shareInBackground()

    val isNetworkCanBeDisabled: Flow<Boolean> = chainNetworkStateFlow.map { it.networkCanBeDisabled }
    val chainEnabled: Flow<Boolean> = chainNetworkStateFlow.map { it.chain.isEnabled }
    val autoBalanceEnabled: Flow<Boolean> = chainNetworkStateFlow.map { it.chain.autoBalanceEnabled }
    val chainModel: Flow<ChainUi> = chainNetworkStateFlow.map { mapChainToUi(it.chain) }

    val customNodes: Flow<List<NetworkConnectionRvItem>> = chainNetworkStateFlow.map { chainNetworkState ->
        buildList {
            val nodes = chainNetworkState.nodeHealthStates.filter { it.node.isCustom }
                .map { mapNodeToUi(it, chainNetworkState) }

            add(NetworkNodesAddCustomRvItem())
            addAll(nodes)
        }
    }

    val defaultNodes: Flow<List<NetworkConnectionRvItem>> = chainNetworkStateFlow.map { chainNetworkState ->
        chainNetworkState.nodeHealthStates.filter { !it.node.isCustom }
            .map { mapNodeToUi(it, chainNetworkState) }
    }

    fun backClicked() {
        router.back()
    }

    fun chainEnableClicked() {
        launch {
            networkManagementChainInteractor.toggleChainEnableState(payload.chainId)
        }
    }

    fun autoBalanceClicked() {
        launch {
            networkManagementChainInteractor.toggleAutoBalance(payload.chainId)
        }
    }

    fun selectNode(item: NetworkNodeRvItem) {
        launch {
            networkManagementChainInteractor.selectNode(payload.chainId, item.socketAddress)
        }
    }

    fun editNode(item: NetworkNodeRvItem) {
        showError("Not implemented")
    }

    fun addNewNode() {
        showError("Not implemented")
    }

    private fun mapNodeToUi(nodeHealthState: NodeHealthState, networkState: ChainNetworkState): NetworkNodeRvItem {
        val selectingAvailable = !networkState.chain.autoBalanceEnabled && networkState.chain.isEnabled

        return NetworkNodeRvItem(
            id = nodeHealthState.node.unformattedUrl,
            name = nodeHealthState.node.name,
            socketAddress = nodeHealthState.node.unformattedUrl,
            isEditable = nodeHealthState.node.isCustom,
            isSelected = nodeHealthState.node.unformattedUrl == networkState.connectingNode?.unformattedUrl,
            connectionState = mapConnectionStateToUi(nodeHealthState),
            isSelectable = selectingAvailable,
            nameColorRes = if (selectingAvailable) R.color.text_primary else R.color.text_secondary
        )
    }

    private fun mapConnectionStateToUi(nodeHealthState: NodeHealthState): ConnectionStateModel {
        return when (val state = nodeHealthState.state) {
            NodeHealthState.State.Connecting -> ConnectionStateModel(
                name = resourceManager.getString(R.string.common_connecting),
                chainStatusColor = resourceManager.getColor(R.color.text_secondary),
                chainStatusIcon = R.drawable.ic_connection_status_connecting,
                chainStatusIconColor = resourceManager.getColor(R.color.icon_secondary),
                showShimmering = true
            )

            is NodeHealthState.State.Connected -> {
                val (iconRes, textColorRes) = when {
                    state.ms < 99 -> R.drawable.ic_connection_status_good to R.color.text_positive
                    state.ms < 499 -> R.drawable.ic_connection_status_average to R.color.text_warning
                    else -> R.drawable.ic_connection_status_bad to R.color.text_negative
                }

                ConnectionStateModel(
                    name = resourceManager.getString(R.string.common_connected_ms, state.ms),
                    chainStatusColor = resourceManager.getColor(textColorRes),
                    chainStatusIcon = iconRes,
                    chainStatusIconColor = null,
                    showShimmering = false
                )
            }

            NodeHealthState.State.Disabled -> ConnectionStateModel(
                name = null,
                chainStatusColor = null,
                chainStatusIcon = R.drawable.ic_connection_status_connecting,
                chainStatusIconColor = resourceManager.getColor(R.color.icon_inactive),
                showShimmering = false
            )
        }
    }
}
