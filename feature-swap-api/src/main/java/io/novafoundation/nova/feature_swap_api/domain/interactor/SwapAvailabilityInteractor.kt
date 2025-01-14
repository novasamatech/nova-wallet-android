package io.novafoundation.nova.feature_swap_api.domain.interactor

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SwapAvailabilityInteractor {

    suspend fun sync(coroutineScope: CoroutineScope)

    suspend fun warmUpCommonlyUsedChains(computationScope: CoroutineScope)

    fun anySwapAvailableFlow(): Flow<Boolean>

    suspend fun swapAvailableFlow(asset: Chain.Asset, coroutineScope: CoroutineScope): Flow<Boolean>
}
