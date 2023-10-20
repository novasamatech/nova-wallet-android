package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider.CollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import kotlinx.coroutines.CoroutineScope

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

    suspend fun create(stakingOption: StakingOption, scope: CoroutineScope) = computationalCache.useCache(COLLATORS_CACHE, scope) {
        val collators = collatorProvider.getCollators(stakingOption, CollatorSource.Elected)

        CollatorRecommendator(collators)
    }
}
