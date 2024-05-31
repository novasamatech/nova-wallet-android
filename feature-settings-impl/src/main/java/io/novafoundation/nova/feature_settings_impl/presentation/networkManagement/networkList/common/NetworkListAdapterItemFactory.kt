package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.domain.NetworkState
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items.NetworkListNetworkRvItem
import io.novafoundation.nova.runtime.ext.isDisabled
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine

interface NetworkListAdapterItemFactory {

    fun getNetworkItem(network: NetworkState): NetworkListNetworkRvItem
}

class RealNetworkListAdapterItemFactory(
    private val resourceManager: ResourceManager
) : NetworkListAdapterItemFactory {

    override fun getNetworkItem(network: NetworkState): NetworkListNetworkRvItem {
        val chain = network.chain
        val subtitle = if (chain.isDisabled) resourceManager.getString(R.string.common_disabled) else null
        val label = getChainLabel(chain)
        return NetworkListNetworkRvItem(
            chainIcon = chain.icon.asIcon(),
            chainId = chain.id,
            title = chain.name,
            subtitle = subtitle,
            chainLabel = label,
            disabled = chain.isDisabled,
            status = getConnectingState(network)
        )
    }

    private fun getChainLabel(chain: Chain): String? {
        return if (chain.isTestNet) {
            resourceManager.getString(R.string.common_testnet)
        } else {
            null
        }
    }

    private fun getConnectingState(network: NetworkState): NetworkListNetworkRvItem.ConnectionStateModel? {
        if (network.chain.isDisabled) return null

        return when (network.connectionState) {
            is SocketStateMachine.State.Connected -> null

            else -> NetworkListNetworkRvItem.ConnectionStateModel(
                name = resourceManager.getString(R.string.common_connecting),
                chainStatusColor = resourceManager.getColor(R.color.text_primary),
                chainStatusIcon = R.drawable.ic_connection_status,
                chainStatusIconColor = resourceManager.getColor(R.color.icon_primary)
            )
        }
    }
}
