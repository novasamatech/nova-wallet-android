package io.novafoundation.nova.common.utils.formatting.duration

import android.content.Context
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.lastDays
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class DayDurationFormatter(
    private val context: Context
) : BoundedDurationFormatter {

    override val threshold: Duration = 1.days

    override fun format(duration: Duration): String {
        val days = duration.lastDays
        return context.resources.getQuantityString(R.plurals.staking_main_lockup_period_value, days.toInt(), days)
    }
}
