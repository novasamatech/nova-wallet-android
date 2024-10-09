package io.novafoundation.nova.common.utils.formatting.duration

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TimeDurationFormatter(
    private val useHours: Boolean = true,
    private val useMinutes: Boolean = true,
    private val useSeconds: Boolean = true
) : BoundedDurationFormatter {

    override val threshold: Duration = 1.minutes

    init {
        if (!useHours && !useMinutes && !useSeconds) throw IllegalArgumentException("At least one of the flags should be true")
    }

    override fun format(duration: Duration): String {
        return duration.toComponents { _, hours, minutes, seconds, _ ->
            val args = listOfNotNull(
                hours.takeIf { useHours },
                minutes.takeIf { useMinutes },
                seconds.takeIf { useSeconds }
            )

            formatTime(*args.toTypedArray())
        }
    }

    private fun formatTime(vararg args: Any): String {
        val formatString = args.joinToString(separator = ":") { "%02d" } // Get string like %02d:%02d:%02d that depends on the number of args

        return formatString.format(*args)
    }
}
