package io.novafoundation.nova.common.utils.formatting.duration

import kotlin.time.Duration

class StringDurationFormatter(
    private val string: String
) : DurationFormatter {

    override fun format(duration: Duration): String {
        return string
    }
}
