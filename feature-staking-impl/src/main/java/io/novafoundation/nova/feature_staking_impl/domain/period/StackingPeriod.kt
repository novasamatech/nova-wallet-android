package io.novafoundation.nova.feature_staking_impl.domain.period

import java.util.Date

sealed interface StackingPeriod {

    object AllTime : StackingPeriod

    object Week : StackingPeriod

    object Month : StackingPeriod

    object Quarter : StackingPeriod

    object HalfYear : StackingPeriod

    object Year : StackingPeriod

    data class Custom(val start: TimePoint, val end: TimePoint) : StackingPeriod {

        sealed interface TimePoint {

            object NoThreshold : TimePoint

            data class Threshold(val value: Date) : TimePoint
        }
    }
}
