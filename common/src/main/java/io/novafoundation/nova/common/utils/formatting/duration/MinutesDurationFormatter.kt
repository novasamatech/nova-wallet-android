package io.novafoundation.nova.common.utils.formatting.duration

import android.content.Context
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.lastMinutes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class MinutesDurationFormatter(
    private val context: Context
) : BoundedDurationFormatter {

    override val threshold: Duration = 1.minutes

    override fun format(duration: Duration): String {
        val minutes = duration.lastMinutes
        return context.resources.getQuantityString(R.plurals.common_minutes_format, minutes, minutes)
    }
}
