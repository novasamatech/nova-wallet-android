package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorSorting
import kotlinx.parcelize.Parcelize

@Parcelize
class CollatorRecommendationConfigParcelModel(val sorting: CollatorSorting) : Parcelable

fun mapCollatorRecommendationConfigToParcel(collatorRecommendationConfig: CollatorRecommendationConfig): CollatorRecommendationConfigParcelModel {
    return with(collatorRecommendationConfig) {
        CollatorRecommendationConfigParcelModel(sorting)
    }
}

fun mapCollatorRecommendationConfigFromParcel(collatorRecommendationConfig: CollatorRecommendationConfigParcelModel): CollatorRecommendationConfig {
    return with(collatorRecommendationConfig) {
        CollatorRecommendationConfig(sorting)
    }
}
