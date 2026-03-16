package io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool

import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

val FORCED_NOVA_POOL_CHAIN_IDS = setOf(
    ChainGeneses.POLKADOT_ASSET_HUB,
    ChainGeneses.KUSAMA_ASSET_HUB
)

interface KnownNovaPools {

    val novaPoolIds: Set<Pair<ChainId, PoolId>>
}

fun KnownNovaPools.isNovaPool(chainId: ChainId, poolId: PoolId) = chainId to poolId in novaPoolIds

fun KnownNovaPools.novaPoolIdForChain(chainId: ChainId): PoolId? =
    novaPoolIds.firstOrNull { it.first == chainId }?.second

class FixedKnownNovaPools : KnownNovaPools {

    override val novaPoolIds: Set<Pair<ChainId, PoolId>> = setOf(
        key(Chain.Geneses.POLKADOT_ASSET_HUB, 54),
        key(Chain.Geneses.KUSAMA_ASSET_HUB, 160),
        key(Chain.Geneses.ALEPH_ZERO, 74),
        key(Chain.Geneses.VARA, 65),
        key(Chain.Geneses.AVAIL, 3)
    )

    private fun key(chainId: ChainId, poolId: Int) = chainId to PoolId(poolId)
}
