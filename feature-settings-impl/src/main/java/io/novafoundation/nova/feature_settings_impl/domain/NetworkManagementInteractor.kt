package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.data.repository.BannerVisibilityRepository
import io.novafoundation.nova.common.utils.filterList
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
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
        return chainRegistry.currentChains
            .asNetworkState()
    }

    override fun addedNetworksFlow(): Flow<List<NetworkState>> {
        return chainRegistry.currentChains
            .filterList { it.isCustomNetwork }
            .asNetworkState()
    }

    private fun Flow<List<Chain>>.asNetworkState(): Flow<List<NetworkState>> {
        return mapList {
            val connection = chainRegistry.getConnectionInstantlyOrNull(it.id)
            NetworkState(it, connection?.getCurrentState())
        }
            .map { sortChains(it) }
    }

    private fun asNetworkState() {

    }

    private fun sortChains(chains: List<NetworkState>): List<NetworkState> {
        return chains.sortedWith(Chain.defaultComparatorFrom(NetworkState::chain))
    }
}
