package io.novafoundation.nova.feature_staking_impl.domain.recommendations

import io.novafoundation.nova.common.utils.applyFilters
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettings
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSorting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ValidatorRecommender(
    val availableValidators: List<Validator>,
    private val novaValidatorIds: Set<String>,
) {

    suspend fun recommendations(settings: RecommendationSettings) = withContext(Dispatchers.Default) {
        val all = availableValidators.applyFiltersAdaptingToEmptyResult(settings.allFilters)
            .sortedWith(settings.sorting)

        val postprocessed = settings.postProcessors.fold(all) { acc, postProcessor ->
            postProcessor.apply(acc)
        }

        if (settings.limit != null) {
            postprocessed.applyLimit(settings.limit, settings.sorting)
        } else {
            postprocessed
        }
    }

    private fun List<Validator>.applyLimit(limit: Int, sorting: RecommendationSorting): List<Validator> {
        if (isEmpty()) return emptyList()

        val (novaValidators, others) = partition { it.accountIdHex in novaValidatorIds }
        val cappedNovaValidators = novaValidators.take(limit)

        val cappedOthers = others.take(limit - cappedNovaValidators.size)

        return (cappedNovaValidators + cappedOthers).sortedWith(sorting)
    }

    private fun List<Validator>.applyFiltersAdaptingToEmptyResult(filters: List<RecommendationFilter>): List<Validator> {
        var filtered = applyFilters(filters)

        if (filtered.isEmpty()) {
            val weakenedFilters = filters.filterNot { it.canIgnoreWhenNoApplicableCandidatesFound() }

            filtered = applyFilters(weakenedFilters)
        }

        return filtered
    }
}
