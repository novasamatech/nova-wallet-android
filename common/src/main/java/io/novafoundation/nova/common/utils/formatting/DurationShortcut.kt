package io.novafoundation.nova.common.utils.formatting

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class DurationShortcutFormatter(private val durationShortcuts: List<DurationShortcut>) {

    constructor(vararg durationShortcuts: DurationShortcut) : this(durationShortcuts.toList())

    fun format(duration: Duration, fallback: () -> String): String {
        val durationShortcut = durationShortcuts.firstOrNull { it.invokeCondition(duration) } ?: return fallback()

        return durationShortcut.shortcut(duration)
    }
}

open class DurationShortcut(val shortcut: (Duration) -> String, val invokeCondition: (Duration) -> Boolean) {

    constructor(formatTo: String, condition: (Duration) -> Boolean) : this({ formatTo }, condition)
}

class DayDurationShortcut(shortcut: String) : DurationShortcut(
    formatTo = shortcut,
    condition = { 1.days - it < 1.hours }
)
