package io.novafoundation.nova.feature_staking_impl.domain.period

import io.novafoundation.nova.common.utils.daysToMillis
import java.util.Date
import java.util.concurrent.TimeUnit

sealed interface RewardPeriod {
    val type: RewardPeriodType
    val start: Date?
    val end: Date?

    data class OffsetFromCurrent(
        val offsetMillis: Long,
        override val type: RewardPeriodType
    ) : RewardPeriod {
        override val start: Date? = null
        override val end: Date? = null
    }

    data class CustomRange(override val start: Date, override val end: Date?) : RewardPeriod {
        override val type = RewardPeriodType.CUSTOM
    }

    object AllTime : RewardPeriod {
        override val type = RewardPeriodType.ALL_TIME
        override val start: Date? = null
        override val end: Date? = null
    }

    companion object {
        fun getOffsetByType(type: RewardPeriodType): Long {
            return when (type) {
                RewardPeriodType.WEEK -> 7.daysToMillis()
                RewardPeriodType.MONTH -> 30.daysToMillis()
                RewardPeriodType.QUARTER -> 90.daysToMillis()
                RewardPeriodType.HALF_YEAR -> 180.daysToMillis()
                RewardPeriodType.YEAR -> 365.daysToMillis()
                else -> -1
            }
        }
    }
}

enum class RewardPeriodType {
    ALL_TIME,
    WEEK,
    MONTH,
    QUARTER,
    HALF_YEAR,
    YEAR,
    CUSTOM
}

fun RewardPeriod.getPeriodDays(): Long {
    return when (this) {
        is RewardPeriod.OffsetFromCurrent -> TimeUnit.MILLISECONDS.toDays(offsetMillis)
        is RewardPeriod.CustomRange -> {
            val endTime = end ?: Date()
            TimeUnit.MILLISECONDS.toDays(endTime.time - start.time)
        }
        else -> -1
    }
}
