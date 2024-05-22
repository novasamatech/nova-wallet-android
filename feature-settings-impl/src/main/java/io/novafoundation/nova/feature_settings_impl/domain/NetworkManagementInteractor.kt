package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetworkState(
    val chain: Chain,
    val connectionState: SocketStateMachine.State?
)

interface NetworkManagementInteractor {

    fun defaultNetworksFlow(): Flow<List<NetworkState>>
}

class RealNetworkManagementInteractor(
    private val chainRegistry: ChainRegistry
) : NetworkManagementInteractor {

    override fun defaultNetworksFlow(): Flow<List<NetworkState>> {
        return chainRegistry.currentChains
            .mapList {
                val connection = chainRegistry.getConnectionInstantlyOrNull(it.id)
                NetworkState(it, connection?.getCurrentState())
            }
            .map { sortChains(it) }
    }

    private fun sortChains(chains: List<NetworkState>): List<NetworkState> {
        return chains.sortedWith(Chain.defaultComparatorFrom(NetworkState::chain))
    }
}
