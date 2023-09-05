package io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface PoolImageDataSource {

    suspend fun getPoolIcon(poolId: PoolId, chainId: ChainId): Icon?
}

class PredefinedPoolImageDataSource(
    knownNovaPools: KnownNovaPools,
) : PoolImageDataSource {

    private val presets: Map<Pair<ChainId, PoolId>, Icon?> = knownNovaPools.novaPoolIds
        .associateWith { R.drawable.ic_nova_logo.asIcon() }

    override suspend fun getPoolIcon(poolId: PoolId, chainId: ChainId): Icon? {
        val key = key(poolId, chainId)
        return presets[key]
    }

    @Suppress("SameParameterValue")
    private fun key(poolId: PoolId, chainId: ChainId) = chainId to poolId
}
