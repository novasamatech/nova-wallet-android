package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter

object NotSlashedFilter : RecommendationFilter {

    override fun canIgnoreWhenNoApplicableCandidatesFound(): Boolean {
        return true
    }

    override fun shouldInclude(model: Validator): Boolean {
        return !model.slashed
    }
}
