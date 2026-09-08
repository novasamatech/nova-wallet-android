package io.novafoundation.nova.runtime.multiNetwork.chain

import android.util.Log
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.FullAssetIdLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.DefaultAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.DefaultAssetsRemote
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val PREF_DEFAULT_ASSETS_APPLIED = "DEFAULT_ASSETS_APPLIED"

/**
 * The curated token list, in the order the config declares it.
 *
 * The list keeps being refreshed after the first launch even though it is only *applied* once:
 * Manage tokens shows the current list as its own section, so changing the config moves tokens
 * between sections for everyone, without turning anything on behind the user's back.
 */
class DefaultAssetsRepository(
    private val chainAssetDao: ChainAssetDao,
    private val chainFetcher: ChainFetcher,
    private val bundledDefaultAssets: BundledDefaultAssets,
    private val preferences: Preferences,
) {

    private val mutex = Mutex()

    private var cachedEnabling: InitialAssetEnabling? = null

    private var cachedDefaults: List<DefaultAssetRemote>? = null

    /**
     * Default assets in config order. Empty only when neither the remote nor the bundled config
     * could be read, which is a broken build rather than a missing network.
     */
    suspend fun defaultAssetIds(): List<FullChainAssetId> = mutex.withLock {
        loadDefaults().map { FullChainAssetId(it.chainId, it.assetId) }
    }

    suspend fun initialAssetEnabling(): InitialAssetEnabling = mutex.withLock {
        cachedEnabling ?: resolveEnabling().also { cachedEnabling = it }
    }

    private suspend fun resolveEnabling(): InitialAssetEnabling {
        if (preferences.getBoolean(PREF_DEFAULT_ASSETS_APPLIED, false)) return InitialAssetEnabling.AlreadyApplied

        // An install that already has assets predates this feature. Its token list is whatever the
        // user has been looking at, so it is marked as applied and left untouched.
        if (chainAssetDao.hasAnyAsset()) {
            preferences.putBoolean(PREF_DEFAULT_ASSETS_APPLIED, true)

            return InitialAssetEnabling.AlreadyApplied
        }

        val defaults = loadDefaults().ifEmpty { return InitialAssetEnabling.Unavailable }

        preferences.putBoolean(PREF_DEFAULT_ASSETS_APPLIED, true)

        val defaultIds = defaults.mapTo(mutableSetOf()) { FullAssetIdLocal(it.chainId, it.assetId) }

        return InitialAssetEnabling.ApplyDefaults(defaultIds)
    }

    private suspend fun loadDefaults(): List<DefaultAssetRemote> {
        cachedDefaults?.let { return it }

        val loaded = remoteDefaults() ?: bundledDefaults() ?: emptyList()
        cachedDefaults = loaded

        return loaded
    }

    private suspend fun remoteDefaults() = runCatching { chainFetcher.getDefaultAssets() }
        .onFailure { Log.e(LOG_TAG, "Failed to fetch the default assets config, falling back to the bundled one: $it") }
        .getOrNull()
        ?.usableDefaults()

    private fun bundledDefaults() = runCatching { bundledDefaultAssets.read() }
        .onFailure { Log.e(LOG_TAG, "Failed to read the bundled default assets config: $it") }
        .getOrNull()
        ?.usableDefaults()

    /**
     * An empty list would hide every token, which is never what the config means to say - treat it
     * as a broken config and let the next source have a go.
     */
    private fun DefaultAssetsRemote.usableDefaults() = defaultAssets.takeIf { it.isNotEmpty() }
}
