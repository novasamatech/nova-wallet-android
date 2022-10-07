package io.novafoundation.nova.common.utils.formatting

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TimerValue(
    val millis: Long,
    val millisCalculatedAt: Long, // used to offset timer value if timer is rerun, e.g. in the RecyclerView
) {
    companion object {

        fun fromCurrentTime(millis: Long): TimerValue {
            return TimerValue(millis, System.currentTimeMillis())
        }
    }

    override fun toString(): String {
        return millis.toDuration(DurationUnit.MILLISECONDS).toString()
    }
}

fun Duration.toTimerValue() = TimerValue.fromCurrentTime(millis = inWholeMilliseconds)

fun TimerValue.remainingTime(): Long {
    val currentTimer = System.currentTimeMillis()
    val passedTime = currentTimer - millisCalculatedAt
    val remainingTime = millis - passedTime

    return remainingTime.coerceAtLeast(0)
}
