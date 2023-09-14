package io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool

import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface KnownNovaPools {

    val novaPoolIds: Set<Pair<ChainId, PoolId>>
}

fun KnownNovaPools.isNovaPool(chainId: ChainId, poolId: PoolId) = chainId to poolId in novaPoolIds

class FixedKnownNovaPools : KnownNovaPools {

    override val novaPoolIds: Set<Pair<ChainId, PoolId>> = setOf(
        key(Chain.Geneses.POLKADOT, 54),
        key(Chain.Geneses.KUSAMA, 160),
        key(Chain.Geneses.ALEPH_ZERO, 74)
    )

    private fun key(chainId: ChainId, poolId: Int) = chainId to PoolId(poolId)
}
