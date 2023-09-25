package io.novafoundation.nova.common.utils.formatting.duration

import io.novafoundation.nova.common.utils.lastHours
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class DayAndHourDurationFormatter(
    private val dayFormatter: DayDurationFormatter,
    private val hoursFormatter: HoursDurationFormatter,
    private val format: String? = null
) : BoundedDurationFormatter {

    override val threshold: Duration = 1.days

    override fun format(duration: Duration): String {
        if (duration.lastHours > 0) {
            return formatDaysAndHours(duration)
        } else {
            return dayFormatter.format(duration)
        }
    }

    private fun formatDaysAndHours(duration: Duration): String {
        if (format == null) {
            return dayFormatter.format(duration) + " " + hoursFormatter.format(duration)
        } else {
            return format.format(
                dayFormatter.format(duration),
                hoursFormatter.format(duration)
            )
        }
    }
}
