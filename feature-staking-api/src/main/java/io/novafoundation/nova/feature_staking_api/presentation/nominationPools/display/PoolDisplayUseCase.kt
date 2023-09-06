package io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface PoolDisplayUseCase {

    suspend fun getPoolDisplay(poolId: Int, chain: Chain): PoolDisplayModel
}
