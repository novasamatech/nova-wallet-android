package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.parachainInfoOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ParachainInfoRepository {

    suspend fun paraId(chainId: ChainId): ParaId?
}

internal class RealParachainInfoRepository(
    private val remoteStorageSource: StorageDataSource,
) : ParachainInfoRepository {

    private val paraIdCacheMutex = Mutex()
    private val paraIdCache = mutableMapOf<ChainId, ParaId?>()

    override suspend fun paraId(chainId: ChainId): ParaId? = paraIdCacheMutex.withLock {
        if (chainId in paraIdCache) {
            paraIdCache.getValue(chainId)
        } else {
            remoteStorageSource.query(chainId) {
                runtime.metadata.parachainInfoOrNull()?.storage("ParachainId")?.query(binding = ::bindNumber)
            }
                .also { paraIdCache[chainId] = it }
        }
    }
}
