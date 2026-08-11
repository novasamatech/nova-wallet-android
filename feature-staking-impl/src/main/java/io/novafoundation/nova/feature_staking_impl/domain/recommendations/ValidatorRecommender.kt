package io.novafoundation.nova.feature_staking_impl.domain.recommendations

import io.novafoundation.nova.common.utils.applyFilters
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_api.domain.model.isLockEligible
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettings
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSorting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ValidatorRecommender(
    val availableValidators: List<Validator>,
    private val novaValidatorIds: Set<String>,
    private val excludedValidators: Set<String>,
) {

    val lockedValidators: List<Validator> = availableValidators.filter {
        it.accountIdHex in novaValidatorIds && it.isLockEligible && it.accountIdHex !in excludedValidators
    }

    private val lockedValidatorIds = lockedValidators.mapToSet { it.accountIdHex }

    suspend fun recommendations(settings: RecommendationSettings) = withContext(Dispatchers.Default) {
        val community = availableValidators.filterNot { it.accountIdHex in lockedValidatorIds }
            .applyFiltersAdaptingToEmptyResult(settings.allFilters)
            .filterExcludedIfNeeded(settings)
            .sortedWith(settings.sorting)

        val postprocessed = settings.postProcessors.fold(community) { acc, postProcessor ->
            postProcessor.apply(acc)
        }

        if (settings.limit != null) {
            postprocessed.reservingSlotsForLocked(settings.limit, settings.sorting)
        } else {
            postprocessed + lockedValidators.sortedWith(settings.sorting)
        }
    }

    private fun List<Validator>.reservingSlotsForLocked(limit: Int, sorting: RecommendationSorting): List<Validator> {
        val reserved = lockedValidators.sortedWith(sorting).take(limit)
        val communityLimit = limit - reserved.size

        if (communityLimit <= 0) return reserved

        return take(communityLimit) + reserved
    }

    private fun List<Validator>.applyFiltersAdaptingToEmptyResult(filters: List<RecommendationFilter>): List<Validator> {
        var filtered = applyFilters(filters)

        if (filtered.isEmpty()) {
            val weakenedFilters = filters.filterNot { it.canIgnoreWhenNoApplicableCandidatesFound() }

            filtered = applyFilters(weakenedFilters)
        }

        return filtered
    }

    private fun List<Validator>.filterExcludedIfNeeded(settings: RecommendationSettings): List<Validator> {
        if (!settings.filterExcluded) return this

        return filter { it.accountIdHex !in excludedValidators }
    }
}
