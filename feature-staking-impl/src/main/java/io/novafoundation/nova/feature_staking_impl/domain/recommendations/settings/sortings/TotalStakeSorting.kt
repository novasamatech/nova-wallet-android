package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSorting

object TotalStakeSorting : RecommendationSorting by Comparator.comparing({ validator: Validator ->
    validator.electedInfo?.totalStake.orZero()
}).reversed()
