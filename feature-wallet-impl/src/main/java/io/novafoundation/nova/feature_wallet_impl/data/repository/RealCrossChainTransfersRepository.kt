package io.novafoundation.nova.feature_wallet_impl.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_impl.data.mappers.crosschain.toDomain
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.CrossChainConfigApi
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainTransfersConfigRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyCrossChainTransfersConfigRemote
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.withContext

private const val LEGACY_CACHE_NAME = "RealCrossChainTransfersRepository.CrossChainConfig"
private const val DYNAMIC_CACHE_NAME = "RealCrossChainTransfersRepository.DynamicCrossChainConfig"

class RealCrossChainTransfersRepository(
    private val api: CrossChainConfigApi,
    private val fileCache: FileCache,
    private val gson: Gson,
    private val parachainInfoRepository: ParachainInfoRepository,
) : CrossChainTransfersRepository {

    override suspend fun syncConfiguration() = withContext(Dispatchers.IO) {
        val legacy = syncConfiguration(LEGACY_CACHE_NAME) { api.getLegacyCrossChainConfig() }
        val dynamic = syncConfiguration(DYNAMIC_CACHE_NAME) { api.getDynamicCrossChainConfig() }

        legacy.await()
        dynamic.await()
    }

    override fun configurationFlow(): Flow<CrossChainTransfersConfiguration> {
        val legacyFlow = fileCache.observeCachedValue(LEGACY_CACHE_NAME).map {
            val remote = gson.fromJson<LegacyCrossChainTransfersConfigRemote>(it)
            remote.toDomain()
        }

        val dynamicFlow = fileCache.observeCachedValue(DYNAMIC_CACHE_NAME).map {
            val remote = gson.fromJson<DynamicCrossChainTransfersConfigRemote>(it)
            remote.toDomain(parachainInfoRepository)
        }

        return dynamicFlow.zip(legacyFlow, ::CrossChainTransfersConfiguration)
    }

    override suspend fun getConfiguration(): CrossChainTransfersConfiguration {
        return withContext(Dispatchers.Default) {
            configurationFlow().first()
        }
    }

    private fun CoroutineScope.syncConfiguration(cacheFileName: String, load: suspend () -> String): Deferred<Unit> {
        return async {
            val raw = retryUntilDone { load() }
            fileCache.updateCache(cacheFileName, raw)
        }
    }
}
