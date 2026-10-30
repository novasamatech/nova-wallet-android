package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal
import java.math.RoundingMode

// Average Gregorian month, so long ages read like a calendar ("33 months") rather than drifting by whole months
private val DAYS_PER_MONTH = BigDecimal("30.436875")

/**
 * Whole calendar months in a subnet age, rounded down. The one month rule shared by the "N months old" chip and the
 * subnet details "Age" row, so the two never disagree (iOS `DateComponentsFormatter` with months as the largest unit).
 */
fun bittensorWholeMonths(ageInDays: BigDecimal): Int {
    return ageInDays.max(BigDecimal.ZERO).divide(DAYS_PER_MONTH, 0, RoundingMode.DOWN).toInt()
}
