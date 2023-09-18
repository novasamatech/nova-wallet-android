package io.novafoundation.nova.common.utils.formatting.duration

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TimeDurationFormatter : BoundedDurationFormatter {

    override val threshold: Duration = 1.minutes

    override fun format(duration: Duration): String {
        return duration.toComponents { _, hours, minutes, seconds, _ ->
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        }
    }
}
