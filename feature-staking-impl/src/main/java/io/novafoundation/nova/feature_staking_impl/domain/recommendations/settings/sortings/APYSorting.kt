package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSorting

object APYSorting : RecommendationSorting by Comparator.comparing({ validator: Validator ->
    validator.electedInfo?.apy.orZero()
}).reversed()
