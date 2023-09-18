package io.novafoundation.nova.common.utils.formatting.duration

import kotlin.time.Duration

interface DurationFormatter {

    fun format(duration: Duration): String
}

interface BoundedDurationFormatter : DurationFormatter {

    val threshold: Duration
}
