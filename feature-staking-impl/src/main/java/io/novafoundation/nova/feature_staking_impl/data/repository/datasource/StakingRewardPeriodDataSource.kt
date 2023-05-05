package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.common.data.storage.Editor
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod.TimePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFS_REWARD_PERIOD = "PREFS_REWARD_PERIOD"
private const val PREFS_CUSTOM_START_TIME = "PREFS_CUSTOM_START_TIME"
private const val PREFS_CUSTOM_END_TIME = "PREFS_CUSTOM_END_TIME"

interface StakingRewardPeriodDataSource {

    fun setRewardPeriod(rewardPeriod: RewardPeriod)

    fun getRewardPeriod(): RewardPeriod

    fun getRewardPeriodFlow(): Flow<RewardPeriod>
}

class RealStakingRewardPeriodDataSource(
    private val preferences: Preferences
) : StakingRewardPeriodDataSource {

    override fun setRewardPeriod(rewardPeriod: RewardPeriod) {
        val prefsEditor = preferences.edit()
        prefsEditor.putString(PREFS_REWARD_PERIOD, mapFromRewardPeriod(rewardPeriod))
        if (rewardPeriod is RewardPeriod.Custom) {
            prefsEditor.setTimePoint(PREFS_CUSTOM_START_TIME, rewardPeriod.start)
            prefsEditor.setTimePoint(PREFS_CUSTOM_END_TIME, rewardPeriod.end)
        } else {
            prefsEditor.remove(PREFS_CUSTOM_START_TIME)
            prefsEditor.remove(PREFS_CUSTOM_END_TIME)
        }
        prefsEditor.apply()
    }

    override fun getRewardPeriod(): RewardPeriod {
        return mapToRewardPeriod(preferences.getString(PREFS_REWARD_PERIOD))
    }

    override fun getRewardPeriodFlow(): Flow<RewardPeriod> {
        return preferences.keysFlow(PREFS_REWARD_PERIOD, PREFS_CUSTOM_START_TIME, PREFS_CUSTOM_END_TIME)
            .map {
                val rewardPeriod = preferences.getString(PREFS_REWARD_PERIOD)
                mapToRewardPeriod(rewardPeriod)
            }
    }

    private fun getTimePeriod(key: String): TimePoint {
        val value = preferences.getLong(key, -1L)
        return if (value == -1L) {
            TimePoint.NoThreshold
        } else {
            TimePoint.Threshold(value)
        }
    }

    private fun Editor.setTimePoint(key: String, timePoint: TimePoint) {
        if (timePoint is TimePoint.Threshold) {
            putLong(key, timePoint.millis)
        } else {
            remove(key)
        }
    }

    private fun mapToRewardPeriod(value: String?): RewardPeriod {
        return when (value) {
            "WEEK" -> RewardPeriod.Week
            "MONTH" -> RewardPeriod.Month
            "QUARTER" -> RewardPeriod.Quarter
            "HALF_YEAR" -> RewardPeriod.HalfYear
            "YEAR" -> RewardPeriod.Year
            "CUSTOM" -> RewardPeriod.Custom(
                getTimePeriod(PREFS_CUSTOM_START_TIME),
                getTimePeriod(PREFS_CUSTOM_END_TIME)
            )
            else -> RewardPeriod.All
        }
    }

    private fun mapFromRewardPeriod(rewardPeriod: RewardPeriod): String {
        return when (rewardPeriod) {
            RewardPeriod.All -> "ALL"
            RewardPeriod.Week -> "WEEK"
            RewardPeriod.Month -> "MONTH"
            RewardPeriod.Quarter -> "QUARTER"
            RewardPeriod.HalfYear -> "HALF_YEAR"
            RewardPeriod.Year -> "YEAR"
            is RewardPeriod.Custom -> "CUSTOM"
        }
    }
}
