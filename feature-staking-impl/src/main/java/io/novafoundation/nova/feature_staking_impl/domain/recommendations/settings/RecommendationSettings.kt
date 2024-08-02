package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings

import io.novafoundation.nova.common.utils.PalletBasedFilter
import io.novafoundation.nova.common.utils.RuntimeDependent
import io.novafoundation.nova.feature_staking_api.domain.model.Validator

interface RecommendationFilter : PalletBasedFilter<Validator> {

    fun canIgnoreWhenNoApplicableCandidatesFound(): Boolean
}

typealias RecommendationSorting = Comparator<Validator>

interface RecommendationPostProcessor : RuntimeDependent {

    fun apply(original: List<Validator>): List<Validator>
}

data class RecommendationSettings(
    val alwaysEnabledFilters: List<RecommendationFilter>,
    val customEnabledFilters: List<RecommendationFilter>,
    val postProcessors: List<RecommendationPostProcessor>,
    val sorting: RecommendationSorting,
    val filterExcluded: Boolean,
    val limit: Int? = null
) {

    val allFilters = alwaysEnabledFilters + customEnabledFilters
}
