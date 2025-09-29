package io.novafoundation.nova.feature_xcm_impl.config

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.flattenKeys
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_xcm_api.config.XcmConfigRepository
import io.novafoundation.nova.feature_xcm_api.config.model.AssetsXcmConfig
import io.novafoundation.nova.feature_xcm_api.config.model.ChainAssetReserveConfig
import io.novafoundation.nova.feature_xcm_api.config.model.ChainXcmConfig
import io.novafoundation.nova.feature_xcm_api.config.model.GeneralXcmConfig
import io.novafoundation.nova.feature_xcm_api.config.remote.toAbsoluteLocation
import io.novafoundation.nova.feature_xcm_impl.config.api.XcmConfigApi
import io.novafoundation.nova.feature_xcm_impl.config.api.response.AssetsXcmConfigRemote
import io.novafoundation.nova.feature_xcm_impl.config.api.response.ChainAssetReserveConfigRemote
import io.novafoundation.nova.feature_xcm_impl.config.api.response.ChainXcmConfigRemote
import io.novafoundation.nova.feature_xcm_impl.config.api.response.GeneralXcmConfigRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@FeatureScope
class RealXcmConfigRepository @Inject constructor(
    private val api: XcmConfigApi,
    private val fileCache: FileCache,
    private val gson: Gson,
    private val rootScope: RootScope,
) : XcmConfigRepository {

    @Volatile
    private var synced: Boolean = false
    private val syncMutex = Mutex()

    private val configCache = singleReplaySharedFlow<GeneralXcmConfig>()

    companion object {
        private const val CACHE_NAME = "RealXcmConfigRepository.Config"
    }

    private suspend fun syncXcmConfig() = syncMutex.withLock {
        if (synced) return@withLock

        val fromCache = fileCache.getCachedValue(CACHE_NAME)
        fromCache?.let(::parseXcmConfig)?.let {
            configCache.emit(it)
        }

        val raw = retryUntilDone { api.getGeneralXcmConfig() }
        fileCache.updateCache(CACHE_NAME, raw)
        raw.let(::parseXcmConfig)?.let {
            configCache.emit(it)
        }

        synced = true
    }

    private fun launchXcmConfigSync() = rootScope.launch { syncXcmConfig() }

    override suspend fun awaitXcmConfig(): GeneralXcmConfig {
        return xcmConfigFlow().first()
    }

    override suspend fun xcmConfigFlow(): Flow<GeneralXcmConfig> {
        launchXcmConfigSync()
        return configCache
    }

    private fun parseXcmConfig(raw: String): GeneralXcmConfig? {
        return runCatching {
            val remote = gson.fromJson<GeneralXcmConfigRemote>(raw)
            remote.toDomain()
        }
            .onFailure { Log.d("XcmConfigRepository", "Failed to parse xcm config", it) }
            .getOrNull()
    }

    private fun GeneralXcmConfigRemote.toDomain(): GeneralXcmConfig {
        return GeneralXcmConfig(
            chains = chains.toDomain(),
            assets = assets.toDomain()
        )
    }

    private fun ChainXcmConfigRemote.toDomain(): ChainXcmConfig {
        return ChainXcmConfig(
            parachainIds = parachainIds
        )
    }

    private fun AssetsXcmConfigRemote.toDomain(): AssetsXcmConfig {
        return AssetsXcmConfig(
            reservesById = reservesById.orEmpty()
                .mapValues { (reserveId, reserve) -> reserve.toDomain(reserveId) },
            assetToReserveIdOverrides = assetToReserveIdOverrides.orEmpty()
                .flattenKeys(::FullChainAssetId)
        )
    }

    private fun ChainAssetReserveConfigRemote.toDomain(reserveId: String): ChainAssetReserveConfig {
        return ChainAssetReserveConfig(
            reserveId = reserveId,
            reserveAssetId = FullChainAssetId(chainId, assetId),
            reserveLocation = multiLocation.toAbsoluteLocation(),
        )
    }
}
