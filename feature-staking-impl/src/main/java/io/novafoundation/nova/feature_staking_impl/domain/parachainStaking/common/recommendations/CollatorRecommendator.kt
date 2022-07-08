package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations

import androidx.lifecycle.Lifecycle
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider.CollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class CollatorRecommendator(private val allCollators: List<Collator>) {

    fun recommendations(config: CollatorRecommendationConfig): List<Collator> {
        return allCollators.sortedWith(config.sorting)
    }
}

private const val COLLATORS_CACHE = "COLLATORS_CACHE"

class CollatorRecommendatorFactory(
    private val collatorProvider: CollatorProvider,
    private val computationalCache: ComputationalCache
) {

    suspend fun create(lifecycle: Lifecycle, chainAsset: Chain.Asset) = computationalCache.useCache(COLLATORS_CACHE, lifecycle) {
        val collators = collatorProvider.getCollators(chainAsset, CollatorSource.Elected)

        CollatorRecommendator(collators)
    }
}
