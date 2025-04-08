package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.bindWeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getStruct
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.getAs
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novafoundation.nova.runtime.network.binding.PerDispatchClassWeight
import io.novafoundation.nova.runtime.network.binding.orZero
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.typed.blockWeight
import io.novafoundation.nova.runtime.storage.typed.system

interface BlockLimitsRepository {

    suspend fun maxWeightForNormalExtrinsics(chainId: ChainId): WeightV2

    suspend fun lastBlockWeight(chainId: ChainId): PerDispatchClassWeight
}

class RealBlockLimitsRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry
) : BlockLimitsRepository {

    override suspend fun maxWeightForNormalExtrinsics(chainId: ChainId): WeightV2 {
        return chainRegistry.withRuntime(chainId) {
            runtime.metadata.system().constant("BlockWeights").getAs(::bindMaxNormalWeight)
        }
    }

    override suspend fun lastBlockWeight(chainId: ChainId): PerDispatchClassWeight {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.system.blockWeight.query().orZero()
        }
    }

    private fun bindMaxNormalWeight(decoded: Any?): WeightV2 {
        val weightRaw = decoded.castToStruct()
            .getStruct("perClass")
            .getStruct("normal")
            .get<Any?>("maxExtrinsic")

        return bindWeightV2(weightRaw)
    }
}
