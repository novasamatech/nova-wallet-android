package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.utils.filterList
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.ext.isCustomNetwork
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private const val INTEGRATE_NETWORKS_BANNER_TAG = "INTEGRATE_NETWORKS_BANNER_TAG"

class NetworkState(
    val chain: Chain,
    val connectionState: SocketStateMachine.State?
)

interface NetworkManagementInteractor {

    fun shouldShowBanner(): Flow<Boolean>

    suspend fun hideBanner()

    fun defaultNetworksFlow(): Flow<List<NetworkState>>

    fun addedNetworksFlow(): Flow<List<NetworkState>>
}

class RealNetworkManagementInteractor(
    private val chainRegistry: ChainRegistry,
    private val bannerVisibilityRepository: BannerVisibilityRepository
) : NetworkManagementInteractor {

    override fun shouldShowBanner(): Flow<Boolean> {
        return bannerVisibilityRepository.shouldShowBannerFlow(INTEGRATE_NETWORKS_BANNER_TAG)
    }

    override suspend fun hideBanner() {
        bannerVisibilityRepository.hideBanner(INTEGRATE_NETWORKS_BANNER_TAG)
    }

    override fun defaultNetworksFlow(): Flow<List<NetworkState>> {
        return networksFlow { !it.isCustomNetwork }
    }

    override fun addedNetworksFlow(): Flow<List<NetworkState>> {
        return networksFlow { it.isCustomNetwork }
    }

    private fun networksFlow(filter: (Chain) -> Boolean) = chainRegistry.currentChains
        .filterList(filter)
        .flatMapLatest { chains ->
            connectionsFlow(sortChains(chains))
        }

    private fun connectionsFlow(chains: List<Chain>): Flow<List<NetworkState>> {
        if (chains.isEmpty()) {
            return flowOf(emptyList())
        }

        return chains.map { chain ->
            val connectionFlow = chainRegistry.getConnectionOrNull(chain.id)?.state ?: flowOf<SocketStateMachine.State?>(SocketStateMachine.State.Disconnected)
            connectionFlow
                .distinctUntilChanged { old, new -> old?.isConnected() == new?.isConnected() }
                .map { state -> NetworkState(chain, state) }
        }.combine()
    }

    private fun sortChains(chains: List<Chain>): List<Chain> {
        return chains.sortedWith(Chain.defaultComparatorFrom { it })
    }

    // It's enough for us to have 2 states for this implementation: connected and not connected
    private fun SocketStateMachine.State.isConnected(): Boolean {
        return this is SocketStateMachine.State.Connected
    }
}
