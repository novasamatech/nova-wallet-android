package io.novafoundation.nova.feature_wallet_impl.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapCrossChainConfigFromRemote
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.CrossChainConfigApi
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.CrossChainTransfersConfigRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val CACHE_NAME = "RealCrossChainTransfersRepository.CrossChainConfig"

class RealCrossChainTransfersRepository(
    private val api: CrossChainConfigApi,
    private val fileCache: FileCache,
    private val gson: Gson
) : CrossChainTransfersRepository {

    override suspend fun syncConfiguration() = withContext(Dispatchers.IO) {
        val rawContents = retryUntilDone { api.getCrossChainConfig() }

        fileCache.updateCache(CACHE_NAME, rawContents)
    }

    override fun configurationFlow() = fileCache.observeCachedValue(CACHE_NAME).map { rawContents ->
        val parsed = gson.fromJson<CrossChainTransfersConfigRemote>(rawContents)

        mapCrossChainConfigFromRemote(parsed)
    }

    override suspend fun getConfiguration(): CrossChainTransfersConfiguration {
        return withContext(Dispatchers.Default) {
            configurationFlow().first()
        }
    }
}
