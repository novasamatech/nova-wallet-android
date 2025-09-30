package io.novafoundation.nova.feature_wallet_impl.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.common.utils.zip
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveRegistry
import io.novafoundation.nova.feature_wallet_impl.data.mappers.crosschain.toDomain
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.CrossChainConfigApi
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainTransfersConfigRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyCrossChainTransfersConfigRemote
import io.novafoundation.nova.feature_xcm_api.config.XcmConfigRepository
import io.novafoundation.nova.feature_xcm_api.converter.LocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val LEGACY_CACHE_NAME = "RealCrossChainTransfersRepository.CrossChainConfig"
private const val DYNAMIC_CACHE_NAME = "RealCrossChainTransfersRepository.DynamicCrossChainConfig"

@FeatureScope
class RealCrossChainTransfersRepository @Inject constructor(
    private val api: CrossChainConfigApi,
    private val fileCache: FileCache,
    private val gson: Gson,
    private val xcmConfigRepository: XcmConfigRepository,
    private val locationConverterFactory: LocationConverterFactory,
    private val chainRegistry: ChainRegistry,
) : CrossChainTransfersRepository {

    override suspend fun syncConfiguration() = withContext(Dispatchers.IO) {
        val legacy = syncConfiguration(LEGACY_CACHE_NAME) { api.getLegacyCrossChainConfig() }
        val dynamic = syncConfiguration(DYNAMIC_CACHE_NAME) { api.getDynamicCrossChainConfig() }

        legacy.await()
        dynamic.await()
    }

    override fun configurationFlow(): Flow<CrossChainTransfersConfiguration> {
        val legacyRemoteFlow = fileCache.observeCachedValue(LEGACY_CACHE_NAME)
            .map { gson.fromJson<LegacyCrossChainTransfersConfigRemote>(it) }

        val dynamicRemoteFlow = fileCache.observeCachedValue(DYNAMIC_CACHE_NAME)
            .map { gson.fromJson<DynamicCrossChainTransfersConfigRemote>(it) }

        return combine(
            tokenReserveRegistryFlow(),
            // Using zip here since syncConfiguration updates both as the same time but not atomically
            // So we want to avoid doubled emissions
            legacyRemoteFlow.zip(dynamicRemoteFlow),
        ) { tokenReserveRegistry, (legacyRemote, dynamicRemote) ->
            CrossChainTransfersConfiguration(
                dynamic = dynamicRemote.toDomain(tokenReserveRegistry),
                legacy = legacyRemote.toDomain(tokenReserveRegistry)
            )
        }
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

    private fun tokenReserveRegistryFlow(): Flow<TokenReserveRegistry> {
        return xcmConfigRepository.xcmConfigFlow().map { xcmGeneralConfig ->
            TokenReserveRegistry(
                xcmConfig = xcmGeneralConfig.assets,
                chainLocationConverter = locationConverterFactory.createChainConverter(),
                chainRegistry = chainRegistry
            )
        }
    }
}
