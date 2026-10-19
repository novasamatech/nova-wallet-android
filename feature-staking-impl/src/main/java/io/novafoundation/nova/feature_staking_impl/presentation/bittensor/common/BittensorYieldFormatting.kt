package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.utils.isNegative
import io.novafoundation.nova.feature_staking_impl.R
import java.math.BigDecimal

@ColorRes
internal fun bittensorYieldColor(annualYield: BigDecimal?): Int = when {
    annualYield == null -> R.color.text_secondary
    annualYield.isNegative -> R.color.text_negative
    else -> R.color.text_positive
}
