package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.getAs
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novafoundation.nova.runtime.network.binding.BlockWeightLimits
import io.novafoundation.nova.runtime.network.binding.PerDispatchClassWeight
import io.novafoundation.nova.runtime.network.binding.orZero
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.typed.blockWeight
import io.novafoundation.nova.runtime.storage.typed.system

interface BlockLimitsRepository {

    suspend fun blockLimits(chainId: ChainId): BlockWeightLimits

    suspend fun lastBlockWeight(chainId: ChainId): PerDispatchClassWeight
}

class RealBlockLimitsRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry
) : BlockLimitsRepository {

    override suspend fun blockLimits(chainId: ChainId): BlockWeightLimits {
        return chainRegistry.withRuntime(chainId) {
            runtime.metadata.system().constant("BlockWeights").getAs(BlockWeightLimits::bind)
        }
    }

    override suspend fun lastBlockWeight(chainId: ChainId): PerDispatchClassWeight {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.system.blockWeight.query().orZero()
        }
    }
}
