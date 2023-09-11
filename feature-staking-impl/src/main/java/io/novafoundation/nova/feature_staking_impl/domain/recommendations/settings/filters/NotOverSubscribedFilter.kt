package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.filters

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationFilter

class NotOverSubscribedFilter(
    private val maxSubscribers: Int
) : RecommendationFilter {

    override fun shouldInclude(model: Validator): Boolean {
        val electedInfo = model.electedInfo

        return if (electedInfo != null) {
            electedInfo.nominatorStakes.size < maxSubscribers
        } else {
            // inactive validators are considered as non-oversubscribed
            true
        }
    }
}
