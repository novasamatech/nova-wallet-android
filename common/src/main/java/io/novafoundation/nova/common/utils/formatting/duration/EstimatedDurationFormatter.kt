package io.novafoundation.nova.common.utils.formatting.duration

import kotlin.time.Duration

class EstimatedDurationFormatter(
    private val durationFormatter: DurationFormatter
) : DurationFormatter {

    override fun format(duration: Duration): String {
        return "~" + durationFormatter.format(duration)
    }
}
