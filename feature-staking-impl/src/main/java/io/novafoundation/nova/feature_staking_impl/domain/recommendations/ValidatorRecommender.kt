package io.novafoundation.nova.feature_staking_impl.domain.recommendations

import io.novafoundation.nova.common.utils.applyFilters
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ValidatorRecommender(val availableValidators: List<Validator>) {

    suspend fun recommendations(settings: RecommendationSettings) = withContext(Dispatchers.Default) {
        val all = availableValidators.applyFilters(settings.allFilters)
            .sortedWith(settings.sorting)

        val postprocessed = settings.postProcessors.fold(all) { acc, postProcessor ->
            postProcessor.apply(acc)
        }

        settings.limit?.let(postprocessed::take) ?: postprocessed
    }
}
