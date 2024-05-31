package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
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
        return chainRegistry.currentChains.flatMapLatest { chains ->
            connectionsFlow(sortChains(chains))
        }
    }

    private fun connectionsFlow(chains: List<Chain>): Flow<List<NetworkState>> {
        return chains.map { chain ->
            val connectionFlow = chainRegistry.getConnectionOrNull(chain.id)?.state ?: emptyFlow<SocketStateMachine.State?>()
            connectionFlow.map { state -> NetworkState(chain, state) }
        }.combine()
    }

    private fun sortChains(chains: List<Chain>): List<Chain> {
        return chains.sortedWith(Chain.defaultComparatorFrom { it })
    }
}
