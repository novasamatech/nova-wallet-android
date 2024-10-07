package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.runtime.ext.isSwapSupported
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.enabledChainsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealSwapAvailabilityInteractor(
    private val chainRegistry: ChainRegistry,
    private val swapService: SwapService
) : SwapAvailabilityInteractor {

    override suspend fun sync(coroutineScope: CoroutineScope) {
        swapService.sync(coroutineScope)
    }

    override fun anySwapAvailableFlow(): Flow<Boolean> {
        return chainRegistry.enabledChainsFlow().map { it.any(Chain::isSwapSupported) }
    }

    override suspend fun swapAvailableFlow(asset: Chain.Asset, coroutineScope: CoroutineScope): Flow<Boolean> {
        return swapService.availableSwapDirectionsFor(asset, coroutineScope)
            .map { it.isNotEmpty() }
    }
}
