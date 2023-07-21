package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

interface ElectionsSession {

    suspend fun currentSlot(chainId: ChainId): BigInteger

    suspend fun genesisSlot(chainId: ChainId): BigInteger

    suspend fun sessionLength(chainId: ChainId): BigInteger

    suspend fun currentEpochIndex(chainId: ChainId): BigInteger?
}
