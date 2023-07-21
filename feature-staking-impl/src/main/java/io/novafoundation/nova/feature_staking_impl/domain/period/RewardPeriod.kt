package io.novafoundation.nova.feature_staking_impl.domain.period

import io.novafoundation.nova.common.utils.atTheNextDay
import io.novafoundation.nova.common.utils.atTheBeginningOfTheDay
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

sealed interface RewardPeriod {

    val type: RewardPeriodType

    val start: Date?

    val end: Date?

    data class OffsetFromCurrent(
        val offset: Duration,
        override val type: RewardPeriodType.Preset
    ) : RewardPeriod {

        override val start: Date
            get() = Date(System.currentTimeMillis() - offset.inWholeMilliseconds).atTheNextDay()

        override val end: Date? = null
    }

    data class CustomRange(override val start: Date, override val end: Date?) : RewardPeriod {
        override val type = RewardPeriodType.Custom
    }

    object AllTime : RewardPeriod {
        override val type = RewardPeriodType.AllTime

        override val start: Date? = null
        override val end: Date? = null
    }

    companion object {
        fun getPresetOffset(type: RewardPeriodType.Preset): Duration {
            val numberOfDays = when (type) {
                RewardPeriodType.Preset.WEEK -> 7
                RewardPeriodType.Preset.MONTH -> 30
                RewardPeriodType.Preset.QUARTER -> 90
                RewardPeriodType.Preset.HALF_YEAR -> 180
                RewardPeriodType.Preset.YEAR -> 365
            }

            return numberOfDays.days
        }
    }
}

sealed interface RewardPeriodType {

    object AllTime : RewardPeriodType

    enum class Preset : RewardPeriodType {
        WEEK,
        MONTH,
        QUARTER,
        HALF_YEAR,
        YEAR
    }

    object Custom : RewardPeriodType
}

fun RewardPeriod.getPeriodDays(): Long {
    return when (this) {
        is RewardPeriod.OffsetFromCurrent -> offset.inWholeDays

        is RewardPeriod.CustomRange -> {
            val endTime = end ?: Date()
            val durationMillis = endTime.atTheNextDay().time - start.atTheBeginningOfTheDay().time

            durationMillis.milliseconds.inWholeDays
        }

        else -> -1
    }
}
