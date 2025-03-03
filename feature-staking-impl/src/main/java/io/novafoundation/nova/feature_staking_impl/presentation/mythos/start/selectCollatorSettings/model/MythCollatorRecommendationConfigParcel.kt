package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorSorting
import kotlinx.android.parcel.Parcelize

@Parcelize
class MythCollatorRecommendationConfigParcel(val sorting: MythosCollatorSorting) : Parcelable

fun MythosCollatorRecommendationConfig.toParcel(): MythCollatorRecommendationConfigParcel {
    return MythCollatorRecommendationConfigParcel(sorting)
}

fun MythCollatorRecommendationConfigParcel.toDomain(): MythosCollatorRecommendationConfig {
    return MythosCollatorRecommendationConfig(sorting)
}
