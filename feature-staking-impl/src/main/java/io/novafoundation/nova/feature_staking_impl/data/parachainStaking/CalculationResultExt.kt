package io.novafoundation.nova.feature_staking_impl.data.parachainStaking

import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator.DurationCalculator

fun DurationCalculator.CalculationResult.toTimerValue(): TimerValue {
    return TimerValue(
        millis = duration.inWholeMilliseconds,
        millisCalculatedAt = calculatedAt
    )
}
