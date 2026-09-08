package io.novafoundation.nova.runtime.multiNetwork.chain

import android.util.Log
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.FullAssetIdLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.DefaultAssetRemote
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
    private val preferences: Preferences,
) {

    private val mutex = Mutex()

    private var cachedEnabling: InitialAssetEnabling? = null

    private var cachedDefaults: List<DefaultAssetRemote>? = null

    /**
     * Default assets in config order, for the Manage tokens section. Best-effort on purpose: a
     * missing config costs the user a section header, which is not worth blocking a screen for.
     */
    suspend fun defaultAssetIds(): List<FullChainAssetId> = mutex.withLock {
        val defaults = cachedDefaults ?: fetchDefaults()?.also { cachedDefaults = it }

        defaults.orEmpty().map { FullChainAssetId(it.chainId, it.assetId) }
    }

    /**
     * Throws when the config cannot be read on a first launch. Callers retry, the same way they
     * already retry the chains config - nothing may be written until the curated list is known,
     * or the wallet ends up looking like an install that predates the feature with no way back.
     */
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

        val defaults = fetchDefaultsOrThrow()

        cachedDefaults = defaults
        preferences.putBoolean(PREF_DEFAULT_ASSETS_APPLIED, true)

        return InitialAssetEnabling.ApplyDefaults(defaults.mapTo(mutableSetOf()) { FullAssetIdLocal(it.chainId, it.assetId) })
    }

    private suspend fun fetchDefaults(): List<DefaultAssetRemote>? {
        return runCatching { fetchDefaultsOrThrow() }
            .onFailure { Log.e(LOG_TAG, "Failed to fetch the default assets config: $it") }
            .getOrNull()
    }

    private suspend fun fetchDefaultsOrThrow(): List<DefaultAssetRemote> {
        val defaults = chainFetcher.getDefaultAssets().defaultAssets

        // An empty list would hide every token, which is never what the config means to say -
        // treat it as broken and let the retry pick up a fixed one.
        return defaults.ifEmpty { error("Default assets config is empty") }
    }
}
