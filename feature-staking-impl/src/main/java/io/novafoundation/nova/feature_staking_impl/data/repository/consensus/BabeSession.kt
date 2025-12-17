package io.novafoundation.nova.feature_staking_impl.data.repository.consensus

import io.novafoundation.nova.common.utils.babe
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.babe
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.currentSlot
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.epochIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.genesisSlot
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.api.queryNonNull
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

class BabeSession(
    private val localStorage: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : ElectionsSession {

    override suspend fun sessionLength(chainId: ChainId): BigInteger {
        return chainRegistry.withRuntime(chainId) {
            metadata.babe().numberConstant("EpochDuration")
        }
    }

    override fun currentEpochIndexFlow(chainId: ChainId): Flow<BigInteger?> {
        return localStorage.subscribe(chainId) {
            runtime.metadata.babe.epochIndex.observe()
        }
    }

    override fun currentSlotFlow(chainId: ChainId) = localStorage.subscribe(chainId) {
        metadata.babe.currentSlot.observeNonNull()
    }

    override suspend fun genesisSlot(chainId: ChainId) = localStorage.query(chainId) {
        metadata.babe.genesisSlot.queryNonNull()
    }

    override suspend fun currentSlotStorageKey(chainId: ChainId): String {
        return chainRegistry.withRuntime(chainId) {
            metadata.babe.currentSlot.storageKey()
        }
    }

    override suspend fun genesisSlotStorageKey(chainId: ChainId): String {
        return chainRegistry.withRuntime(chainId) {
            metadata.babe.genesisSlot.storageKey()
        }
    }

    override suspend fun currentEpochIndexStorageKey(chainId: ChainId): String {
        return chainRegistry.withRuntime(chainId) {
            runtime.metadata.babe.epochIndex.storageKey()
        }
    }
}
