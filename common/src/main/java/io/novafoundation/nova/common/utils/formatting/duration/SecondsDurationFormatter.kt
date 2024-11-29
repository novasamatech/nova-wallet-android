package io.novafoundation.nova.common.utils.formatting.duration

import android.content.Context
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.lastSeconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SecondsDurationFormatter(
    private val context: Context
) : BoundedDurationFormatter {

    override val threshold: Duration = 1.seconds

    override fun format(duration: Duration): String {
        val seconds = duration.lastSeconds
        return context.resources.getQuantityString(R.plurals.common_seconds_format, seconds, seconds)
    }
}
