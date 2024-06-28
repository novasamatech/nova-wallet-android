package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.chain.asIconOrFallback
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.domain.NetworkState
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.common.ConnectionStateModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.NetworkListRvItem
import io.novafoundation.nova.runtime.ext.isDisabled
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.LightChain
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine

interface NetworkListAdapterItemFactory {

    fun getNetworkItem(network: NetworkState): NetworkListRvItem

    fun getNetworkItem(network: LightChain): NetworkListRvItem
}

class RealNetworkListAdapterItemFactory(
    private val resourceManager: ResourceManager
) : NetworkListAdapterItemFactory {

    override fun getNetworkItem(network: NetworkState): NetworkListRvItem {
        val chain = network.chain
        val subtitle = if (chain.isDisabled) resourceManager.getString(R.string.common_disabled) else null
        val label = getChainLabel(chain)
        return NetworkListRvItem(
            chainIcon = chain.iconOrFallback(),
            chainId = chain.id,
            title = chain.name,
            subtitle = subtitle,
            chainLabel = label,
            disabled = chain.isDisabled,
            status = getConnectingState(network)
        )
    }

    override fun getNetworkItem(network: LightChain): NetworkListRvItem {
        return NetworkListRvItem(
            chainIcon = network.icon.asIconOrFallback(),
            chainId = network.id,
            title = network.name,
            subtitle = null,
            chainLabel = null,
            disabled = false,
            status = null
        )
    }

    private fun getChainLabel(chain: Chain): String? {
        return if (chain.isTestNet) {
            resourceManager.getString(R.string.common_testnet)
        } else {
            null
        }
    }

    private fun getConnectingState(network: NetworkState): ConnectionStateModel? {
        if (network.chain.isDisabled) return null

        return when (network.connectionState) {
            is SocketStateMachine.State.Connected -> null

            else -> ConnectionStateModel(
                name = resourceManager.getString(R.string.common_connecting),
                chainStatusColor = resourceManager.getColor(R.color.text_primary),
                chainStatusIcon = R.drawable.ic_connection_status_connecting,
                chainStatusIconColor = resourceManager.getColor(R.color.icon_primary),
                showShimmering = true
            )
        }
    }
}
