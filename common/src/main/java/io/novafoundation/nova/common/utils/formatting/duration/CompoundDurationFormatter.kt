package io.novafoundation.nova.common.utils.formatting.duration

import kotlin.time.Duration

class CompoundDurationFormatter(
    val formatters: List<BoundedDurationFormatter>
) : DurationFormatter {

    constructor(vararg formatters: BoundedDurationFormatter) : this(formatters.toList())

    override fun format(duration: Duration): String {
        val formatter = formatters.firstOrNull { it.threshold <= duration } ?: formatters.last()

        return formatter.format(duration)
    }
}
