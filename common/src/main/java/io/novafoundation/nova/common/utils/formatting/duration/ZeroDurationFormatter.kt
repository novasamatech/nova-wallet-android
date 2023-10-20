package io.novafoundation.nova.common.utils.formatting.duration

import kotlin.time.Duration

class ZeroDurationFormatter(
    private val nestedFormatter: DurationFormatter
) : BoundedDurationFormatter {

    override val threshold: Duration = Duration.ZERO

    override fun format(duration: Duration): String {
        return nestedFormatter.format(duration)
    }
}
