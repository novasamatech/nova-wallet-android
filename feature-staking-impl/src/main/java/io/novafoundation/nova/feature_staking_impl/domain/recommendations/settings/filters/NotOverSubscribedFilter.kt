package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter

object NotOverSubscribedFilter : RecommendationFilter {

    override fun shouldInclude(model: Validator): Boolean {
        val isOversubscribed = model.electedInfo?.isOversubscribed

        return if (isOversubscribed != null) {
            !isOversubscribed
        } else {
            true // inactive validators are considered as non-oversubscribed
        }
    }
}
