package io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface PoolImageDataSource {

    suspend fun getPoolIcon(poolId: PoolId, chainId: ChainId): Icon?
}

private const val NOVA_POLKADOT_POOL_ID = 54

class PredefinedPoolImageDataSource : PoolImageDataSource {

    private val presets: Map<Pair<ChainId, PoolId>, Icon?> = mapOf(
        key(NOVA_POLKADOT_POOL_ID, Chain.Geneses.POLKADOT) to R.drawable.ic_nova_logo.asIcon()
    )

    override suspend fun getPoolIcon(poolId: PoolId, chainId: ChainId): Icon? {
        val key = key(poolId, chainId)
        return presets[key]
    }

    @Suppress("SameParameterValue")
    private fun key(poolId: Int, chainId: ChainId) = key(PoolId(poolId.toBigInteger()), chainId)

    @Suppress("SameParameterValue")
    private fun key(poolId: PoolId, chainId: ChainId) = chainId to poolId
}
