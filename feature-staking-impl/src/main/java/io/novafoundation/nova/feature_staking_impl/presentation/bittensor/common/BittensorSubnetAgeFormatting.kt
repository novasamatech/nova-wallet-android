package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import java.math.BigDecimal
import java.math.RoundingMode

// Average Gregorian month, so long ages read like a calendar ("33 months") rather than drifting by whole months
private val DAYS_PER_MONTH = BigDecimal("30.436875")
private val HOURS_PER_DAY = BigDecimal(24)
private val MINUTES_PER_HOUR = BigDecimal(60)

enum class BittensorAgeUnit { MONTHS, DAYS, HOURS, MINUTES }

data class BittensorAge(val count: Int, val unit: BittensorAgeUnit)

/**
 * The single largest whole unit among months, days, hours and minutes, rounded down ("33 months", "12 days", "5 hours").
 * Mirrors iOS `DateComponentsFormatter` with `maximumUnitCount = 1` used for the subnet age.
 */
fun bittensorSubnetAge(ageInDays: BigDecimal): BittensorAge {
    val days = ageInDays.max(BigDecimal.ZERO)
    val months = days.divide(DAYS_PER_MONTH, 0, RoundingMode.DOWN).toInt()
    if (months > 0) return BittensorAge(months, BittensorAgeUnit.MONTHS)

    val wholeDays = days.setScale(0, RoundingMode.DOWN).toInt()
    if (wholeDays > 0) return BittensorAge(wholeDays, BittensorAgeUnit.DAYS)

    val hours = (days * HOURS_PER_DAY).setScale(0, RoundingMode.DOWN).toInt()
    if (hours > 0) return BittensorAge(hours, BittensorAgeUnit.HOURS)

    val minutes = (days * HOURS_PER_DAY * MINUTES_PER_HOUR).setScale(0, RoundingMode.DOWN).toInt()
    return BittensorAge(minutes, BittensorAgeUnit.MINUTES)
}

fun ResourceManager.formatBittensorSubnetAge(ageInDays: BigDecimal): String {
    val age = bittensorSubnetAge(ageInDays)
    val pluralRes = when (age.unit) {
        BittensorAgeUnit.MONTHS -> R.plurals.bittensor_age_months
        BittensorAgeUnit.DAYS -> R.plurals.bittensor_age_days
        BittensorAgeUnit.HOURS -> R.plurals.common_hours_format
        BittensorAgeUnit.MINUTES -> R.plurals.common_minutes_format
    }
    return getQuantityString(pluralRes, age.count, age.count)
}
