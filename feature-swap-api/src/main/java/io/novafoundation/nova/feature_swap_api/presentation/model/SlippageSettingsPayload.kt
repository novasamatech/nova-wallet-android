package io.novafoundation.nova.feature_swap_api.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import kotlinx.parcelize.Parcelize

@Parcelize
data class SlippageSettingsPayload(
    val selected: Double,
    val default: Double,
    val tips: List<Double>,
    val minimum: Double,
    val maximum: Double,
    val small: Double,
    val big: Double
) : Parcelable {

    fun config() = SlippageConfig(default.fractions, tips.map { it.fractions }, minimum.fractions, maximum.fractions, small.fractions, big.fractions)
}
