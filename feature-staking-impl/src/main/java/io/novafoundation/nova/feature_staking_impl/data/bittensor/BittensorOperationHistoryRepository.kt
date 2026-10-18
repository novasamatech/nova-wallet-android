package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationHistory
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.math.BigInteger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BittensorOperationHistoryRepository(
    private val remoteSource: BittensorEstimatesRemoteSource,
    private val infrastructureUrls: InfrastructureUrls
) {

    private data class CachedHistory(
        val history: BittensorOperationHistory,
        val validUntilNanos: Long,
        val balanceRevision: BigInteger?
    )

    private val mutex = Mutex()

    private val histories = mutableMapOf<String, CachedHistory>()

    suspend fun history(address: String, balanceRevision: BigInteger? = null): BittensorOperationHistory = mutex.withLock {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val cacheKey = "$baseUrl:$address"
        val cached = histories[cacheKey]
        if (cached != null && System.nanoTime() < cached.validUntilNanos && balanceRevision == cached.balanceRevision) {
            return@withLock cached.history
        }

        val pages = remoteSource.history(baseUrl, address)
        val history = BittensorOperationHistory(pages.items, pages.allPagesRead)
        if (histories.size >= HISTORY_CACHE_LIMIT) histories.clear()
        histories[cacheKey] = CachedHistory(history, pages.validUntilNanos, balanceRevision)
        history
    }

    private companion object {
        const val HISTORY_CACHE_LIMIT = 8
    }
}
