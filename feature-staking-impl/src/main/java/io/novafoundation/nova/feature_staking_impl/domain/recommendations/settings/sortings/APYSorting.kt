package io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.notElected
import java.util.Comparator

object APYSorting : RecommendationSorting by Comparator.comparing({ validator: Validator ->
    validator.electedInfo?.apy ?: notElected(validator.accountIdHex)
}).reversed()
