package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.parasOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage

interface ParasRepository {

    suspend fun activePublicParachains(chainId: ChainId): Int?
}

private val LOWEST_PUBLIC_ID = 2000.toBigInteger()

class RealParasRepository(
    private val localSource: StorageDataSource
) : ParasRepository {

    override suspend fun activePublicParachains(chainId: ChainId): Int? {
        return localSource.query(chainId) {
            val parachains = runtime.metadata.parasOrNull()?.storage("Parachains")
                ?.query(binding = ::bindParachains) ?: return@query null

            parachains.count { it >= LOWEST_PUBLIC_ID }
        }
    }

    private fun bindParachains(decoded: Any?): List<ParaId> {
        return bindList(decoded, ::bindNumber)
    }
}
