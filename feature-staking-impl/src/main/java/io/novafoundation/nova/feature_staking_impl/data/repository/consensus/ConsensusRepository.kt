package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

interface ConsensusRepository {

    suspend fun consensusAvailable(chainId: ChainId): Boolean

    suspend fun currentSlot(chainId: ChainId): BigInteger

    suspend fun genesisSlot(chainId: ChainId): BigInteger

    suspend fun sessionLength(chainId: ChainId): BigInteger
}
