package io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface KnownNovaPools {

    val novaPoolIds: Set<Pair<ChainId, PoolId>>
}

fun KnownNovaPools.isNovaPool(chainId: ChainId, poolId: PoolId) = chainId to poolId in novaPoolIds

private const val NOVA_POLKADOT_POOL_ID = 54

class FixedKnownNovaPools : KnownNovaPools {

    override val novaPoolIds: Set<Pair<ChainId, PoolId>> = setOf(
        key(Chain.Geneses.POLKADOT, NOVA_POLKADOT_POOL_ID)
    )

    private fun key(chainId: ChainId, poolId: Int) = chainId to PoolId(poolId)
}

