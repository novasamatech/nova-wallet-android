package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.data.network.runtime.binding.bindWeight
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getStruct
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource

interface BlockLimitsRepository {

    suspend fun maxWeightForNormalExtrinsics(chainId: ChainId): Weight
}

class RealBlockLimitsRepository(
    private val localStorageDataSource: StorageDataSource
) : BlockLimitsRepository {

    override suspend fun maxWeightForNormalExtrinsics(chainId: ChainId): Weight {
        return localStorageDataSource.query(chainId) {
            runtime.metadata.system().constant("BlockWeights").getAs(::bindMaxNormalWeight)
        }
    }

    private fun bindMaxNormalWeight(decoded: Any?): Weight {
        val weightRaw = decoded.castToStruct()
            .getStruct("perClass")
            .getStruct("normal")
            .get<Any?>("maxExtrinsic")

        return bindWeight(weightRaw)
    }
}
