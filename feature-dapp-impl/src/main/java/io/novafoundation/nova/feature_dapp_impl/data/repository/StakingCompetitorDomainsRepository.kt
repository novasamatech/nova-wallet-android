package io.novafoundation.nova.feature_dapp_impl.data.repository

import android.util.Log
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_impl.data.network.competitors.StakingCompetitorDomainsApi

interface StakingCompetitorDomainsRepository {

    /**
     * Synchronous, sync-readable competitor check. Reads the in-memory cache.
     * Returns false when the cache is empty (fail-open).
     *
     * Must be sync so that WebView's shouldOverrideUrlLoading() — which requires
     * a synchronous Boolean return — can call it.
     */
    fun isStakingCompetitor(url: String): Boolean

    /**
     * One-shot remote fetch. On success, replaces the in-memory cache.
     * On any failure (network, parse, etc.) leaves the cache untouched and logs.
     *
     * Safe to call repeatedly; safe to ignore the result.
     */
    suspend fun sync()
}

class RealStakingCompetitorDomainsRepository(
    private val api: StakingCompetitorDomainsApi,
    private val remoteUrl: String
) : StakingCompetitorDomainsRepository {

    @Volatile
    private var domains: List<String> = emptyList()

    override fun isStakingCompetitor(url: String): Boolean {
        val snapshot = domains
        if (snapshot.isEmpty()) return false

        val host = runCatching { Urls.hostOf(url) }.getOrNull() ?: return false
        val lowered = host.lowercase()

        return snapshot.any { domain ->
            val target = domain.lowercase()
            lowered == target || lowered.endsWith(".$target")
        }
    }

    override suspend fun sync() {
        runCatching {
            val response = api.getDomains(remoteUrl)
            domains = response.domains
        }.onFailure { error ->
            Log.w(TAG, "StakingCompetitorDomainsRepository sync failed", error)
        }
    }

    private companion object {
        const val TAG = "StakingCompetitors"
    }
}
