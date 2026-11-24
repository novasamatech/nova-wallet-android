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
    val big: Double,
    val requestId: String? = null,
    val maxPercentFractionDigits: Int = SlippageConfig.DEFAULT_PERCENT_FRACTION_DIGITS
) : Parcelable {

    fun config() = SlippageConfig(
        defaultSlippage = default.fractions,
        slippageTips = tips.map { it.fractions },
        minAvailableSlippage = minimum.fractions,
        maxAvailableSlippage = maximum.fractions,
        smallSlippage = small.fractions,
        bigSlippage = big.fractions,
        maxPercentFractionDigits = maxPercentFractionDigits
    )
}
