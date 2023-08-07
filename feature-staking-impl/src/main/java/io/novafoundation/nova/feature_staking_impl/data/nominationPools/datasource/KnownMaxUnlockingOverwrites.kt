package io.novafoundation.nova.feature_staking_impl.data.nominationPools.datasource

import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

interface KnownMaxUnlockingOverwrites {

    suspend fun getUnlockChunksFor(chainId: ChainId): BigInteger?
}

class RealKnownMaxUnlockingOverwrites : KnownMaxUnlockingOverwrites {

    private val knownMaxUnlockChunksByChainId = mapOf(
        Chain.Geneses.ALEPH_ZERO to 8.toBigInteger()
    )

    override suspend fun getUnlockChunksFor(chainId: ChainId): BigInteger? {
        return knownMaxUnlockChunksByChainId[chainId]
    }
}
