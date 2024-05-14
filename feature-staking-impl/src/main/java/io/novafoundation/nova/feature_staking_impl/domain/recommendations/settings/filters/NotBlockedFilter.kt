package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter

object NotBlockedFilter : RecommendationFilter {

    override fun canIgnoreWhenNoApplicableCandidatesFound(): Boolean {
        return false
    }

    override fun shouldInclude(model: Validator) = model.prefs?.blocked?.not() ?: false
}
