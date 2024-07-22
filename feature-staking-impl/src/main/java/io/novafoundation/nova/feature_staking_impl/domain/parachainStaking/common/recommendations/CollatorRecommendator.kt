package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.indexOfOrNull
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.validators.ValidatorsPreferencesSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider.CollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import kotlinx.coroutines.CoroutineScope

class CollatorRecommendator(private val allCollators: List<Collator>, private val novaCollatorIds: Set<String>) {

    fun recommendations(config: CollatorRecommendationConfig): List<Collator> {
        return allCollators.sortedWith(config.sorting)
            .sortedBy { novaCollatorIds.indexOfOrNull(it.address) ?: Int.MAX_VALUE }
    }

    fun default(): Collator? {
        val collatorByAddress = allCollators.associateBy { it.address }
        return novaCollatorIds.firstOrNull { collatorByAddress.containsKey(it) }
            ?.let { collatorByAddress.get(it) }
    }
}

private const val COLLATORS_CACHE = "COLLATORS_CACHE"

class CollatorRecommendatorFactory(
    private val collatorProvider: CollatorProvider,
    private val computationalCache: ComputationalCache,
    private val validatorsPreferencesSource: ValidatorsPreferencesSource
) {

    suspend fun create(stakingOption: StakingOption, scope: CoroutineScope) = computationalCache.useCache(COLLATORS_CACHE, scope) {
        val collators = collatorProvider.getCollators(stakingOption, CollatorSource.Elected)

        val knownNovaCollators = validatorsPreferencesSource.getRecommendedValidatorIds(stakingOption.chain.id)

        CollatorRecommendator(collators, knownNovaCollators)
    }
}
