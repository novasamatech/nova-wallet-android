package io.novafoundation.nova.feature_assets.domain.swap

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface SwapNetworkSelectionRepository {
    fun savedNetworks(): Flow<Set<ChainId>>
    fun saveNetworks(networks: Set<ChainId>)
}
