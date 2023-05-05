package io.novafoundation.nova.feature_staking_impl.domain.period

import io.novafoundation.nova.common.utils.daysToMillis
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod.TimePoint
import java.util.Date

sealed class RewardPeriod(
    val start: TimePoint,
    val end: TimePoint
) {

    init {
        if (this.start is TimePoint.ThresholdOffset && end is TimePoint.ThresholdOffset) {
            throw IllegalStateException("invalid data. offset cannot be calculated from another offset")
        }
    }

    object All : RewardPeriod(TimePoint.NoThreshold, TimePoint.NoThreshold)

    object Week : RewardPeriod(7.toThresholdOffset(), TimePoint.NoThreshold)

    object Month : RewardPeriod(30.toThresholdOffset(), TimePoint.NoThreshold)

    object Quarter : RewardPeriod(90.toThresholdOffset(), TimePoint.NoThreshold)

    object HalfYear : RewardPeriod(180.toThresholdOffset(), TimePoint.NoThreshold)

    object Year : RewardPeriod(365.toThresholdOffset(), TimePoint.NoThreshold)

    data class Custom(private val _start: TimePoint, private val _end: TimePoint) : RewardPeriod(_start, _end)

    sealed interface TimePoint {

        object NoThreshold : TimePoint

        data class Threshold(val millis: Long) : TimePoint

        data class ThresholdOffset(val millis: Long) : TimePoint

    }
}

private fun Int.toThresholdOffset(): TimePoint.ThresholdOffset {
    return TimePoint.ThresholdOffset(daysToMillis())
}

fun RewardPeriod.getPeriodMillis(): Long {
    return when (end) {
        is TimePoint.NoThreshold -> when (start) {
            is TimePoint.NoThreshold -> -1
            is TimePoint.Threshold -> Date().time - start.millis
            is TimePoint.ThresholdOffset -> start.millis
        }
        is TimePoint.Threshold -> when (start) {
            is TimePoint.NoThreshold -> -1
            is TimePoint.Threshold -> end.millis - start.millis
            is TimePoint.ThresholdOffset -> start.millis
        }
        is TimePoint.ThresholdOffset -> when (start) {
            is TimePoint.ThresholdOffset -> throw IllegalStateException()
            else -> end.millis
        }
    }
}
