package io.novafoundation.nova.common.utils.formatting.duration

import io.novafoundation.nova.common.utils.lastMinutes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class RoundMinutesDurationFormatter(
    private val nestedDurationFormatter: DurationFormatter,
    private val roundMinutesThreshold: Duration = 1.hours
) : DurationFormatter {

    override fun format(duration: Duration): String {
        val roundedDuration = if (duration > roundMinutesThreshold) {
            roundMinutes(duration)
        } else {
            duration
        }
        return nestedDurationFormatter.format(roundedDuration)
    }

    private fun roundMinutes(duration: Duration): Duration {
        val lastMinutes = duration.lastMinutes
        val wholeMinutes = if (lastMinutes >= 30) {
            duration.inWholeMinutes + (60 - lastMinutes)
        } else {
            duration.inWholeMinutes - lastMinutes
        }

        return wholeMinutes.minutes
    }
}
