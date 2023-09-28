package io.novafoundation.nova.common.utils.formatting.duration

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ShortcutDurationFormatter(
    private val shortcuts: List<DurationShortcut>,
    private val nestedFormatter: BoundedDurationFormatter
) : BoundedDurationFormatter {

    override val threshold: Duration = nestedFormatter.threshold

    override fun format(duration: Duration): String {
        val formatter = shortcuts.firstOrNull { it.invokeCondition(duration) }
            ?.formatter
            ?: nestedFormatter

        return formatter.format(duration)
    }
}

open class DurationShortcut(val formatter: DurationFormatter, val invokeCondition: (Duration) -> Boolean) {

    constructor(formatTo: String, condition: (Duration) -> Boolean) : this(StringDurationFormatter(formatTo), condition)
}

class DayDurationShortcut(shortcut: String) : DurationShortcut(
    formatTo = shortcut,
    condition = { it > 23.hours && it <= 1.days }
)
