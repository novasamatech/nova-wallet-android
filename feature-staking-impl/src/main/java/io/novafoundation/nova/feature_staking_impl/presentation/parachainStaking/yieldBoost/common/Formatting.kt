package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_staking_impl.R

fun ResourceManager.formatDaysFrequency(days: Int): String {
    return if (days == 1) {
        getString(R.string.common_frequency_days_everyday)
    } else {
        getQuantityString(R.plurals.common_frequency_days, days, days.format())
    }
}
