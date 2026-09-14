package io.novafoundation.nova.runtime.multiNetwork.chain

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The curated token list, in the order the config declares it.
 *
 * Purely advisory: it decides what the asset list *shows*, never what is stored or subscribed to.
 * An empty result means "no opinion" and leaves every asset visible.
 */
class DefaultAssetsRepository(
    private val chainFetcher: ChainFetcher
) {

    private val mutex = Mutex()

    private var cached: List<FullChainAssetId>? = null

    suspend fun defaultAssetIds(): List<FullChainAssetId> = mutex.withLock {
        cached ?: fetchDefaultAssetIds().also { if (it.isNotEmpty()) cached = it }
    }

    private suspend fun fetchDefaultAssetIds(): List<FullChainAssetId> {
        val remote = runCatching { chainFetcher.getDefaultAssets() }
            .onFailure { Log.e(LOG_TAG, "Failed to fetch the default assets config: $it") }
            .getOrNull()
            ?: return emptyList()

        return remote.defaultAssets.map { FullChainAssetId(it.chainId, it.assetId) }
    }
}
