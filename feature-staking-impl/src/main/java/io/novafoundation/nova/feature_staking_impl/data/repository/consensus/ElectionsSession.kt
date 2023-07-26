package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface ElectionsSession {

    fun currentSlotFlow(chainId: ChainId): Flow<BigInteger>

    suspend fun genesisSlot(chainId: ChainId): BigInteger

    suspend fun sessionLength(chainId: ChainId): BigInteger

    fun currentEpochIndexFlow(chainId: ChainId): Flow<BigInteger?>

    suspend fun currentSlotStorageKey(chainId: ChainId): String?

    suspend fun genesisSlotStorageKey(chainId: ChainId): String?

    suspend fun currentEpochIndexStorageKey(chainId: ChainId): String?
}
