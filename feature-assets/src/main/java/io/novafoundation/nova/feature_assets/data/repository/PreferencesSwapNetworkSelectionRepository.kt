package io.novafoundation.nova.feature_assets.data.repository

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_assets.domain.swap.SwapNetworkSelectionRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesSwapNetworkSelectionRepository(private val preferences: Preferences) : SwapNetworkSelectionRepository {

    override fun savedNetworks(): Flow<Set<ChainId>> = preferences.stringSetFlow(SAVED_NETWORKS).map { it.orEmpty() }

    override fun saveNetworks(networks: Set<ChainId>) = preferences.putStringSet(SAVED_NETWORKS, networks)

    private companion object {
        const val SAVED_NETWORKS = "SWAP_TOKEN_SELECTION_SAVED_NETWORKS"
    }
}
