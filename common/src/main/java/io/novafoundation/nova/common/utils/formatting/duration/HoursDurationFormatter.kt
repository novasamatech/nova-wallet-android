package io.novafoundation.nova.common.utils.formatting.duration

import android.content.Context
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.lastHours
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class HoursDurationFormatter(
    private val context: Context
) : BoundedDurationFormatter {

    override val threshold: Duration = 1.hours

    override fun format(duration: Duration): String {
        val hours = duration.lastHours
        return context.resources.getQuantityString(R.plurals.common_hours_format, hours, hours)
    }
}
