package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.common.data.storage.Editor
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFS_PERIOD_OFFSET = "PREFS_PERIOD_OFFSET"
private const val PREFS_REWARD_PERIOD = "PREFS_REWARD_PERIOD"
private const val PREFS_CUSTOM_START_TIME = "PREFS_CUSTOM_START_TIME"
private const val PREFS_CUSTOM_END_TIME = "PREFS_CUSTOM_END_TIME"

interface StakingRewardPeriodDataSource {

    fun setRewardPeriod(rewardPeriod: RewardPeriod)

    fun getRewardPeriod(): RewardPeriod

    fun observeRewardPeriod(): Flow<RewardPeriod>
}

class RealStakingRewardPeriodDataSource(
    private val preferences: Preferences
) : StakingRewardPeriodDataSource {

    override fun setRewardPeriod(rewardPeriod: RewardPeriod) {
        val prefsEditor = preferences.edit()
        prefsEditor.putString(PREFS_REWARD_PERIOD, mapFromRewardPeriod(rewardPeriod))
        when (rewardPeriod) {
            is RewardPeriod.CustomRange -> {
                prefsEditor.setTimePoint(PREFS_CUSTOM_START_TIME, rewardPeriod.start)
                prefsEditor.setTimePoint(PREFS_CUSTOM_END_TIME, rewardPeriod.end)
            }
            is RewardPeriod.OffsetFromCurrent -> {
                prefsEditor.putLong(PREFS_PERIOD_OFFSET, rewardPeriod.offsetMillis)
            }
            else -> {
                prefsEditor.remove(PREFS_CUSTOM_START_TIME)
                prefsEditor.remove(PREFS_CUSTOM_END_TIME)
                prefsEditor.remove(PREFS_PERIOD_OFFSET)
            }
        }
        prefsEditor.apply()
    }

    override fun getRewardPeriod(): RewardPeriod {
        val periodType = preferences.getString(PREFS_REWARD_PERIOD)
            ?.let { RewardPeriodType.valueOf(it) }
        return mapToRewardPeriod(periodType)
    }

    override fun observeRewardPeriod(): Flow<RewardPeriod> {
        return preferences.keysFlow(PREFS_REWARD_PERIOD, PREFS_CUSTOM_START_TIME, PREFS_CUSTOM_END_TIME)
            .map { getRewardPeriod() }
    }

    private fun getTimePeriod(key: String): Date? {
        val value = preferences.getLong(key, -1L)
        return if (value == -1L) {
            null
        } else {
            Date(value)
        }
    }

    private fun Editor.setTimePoint(key: String, date: Date?) {
        if (date == null) {
            remove(key)
        } else {
            putLong(key, date.time)
        }
    }

    private fun mapToRewardPeriod(value: RewardPeriodType?): RewardPeriod {
        return when (value) {
            null,
            RewardPeriodType.ALL_TIME -> RewardPeriod.AllTime
            RewardPeriodType.WEEK,
            RewardPeriodType.MONTH,
            RewardPeriodType.QUARTER,
            RewardPeriodType.HALF_YEAR,
            RewardPeriodType.YEAR -> RewardPeriod.OffsetFromCurrent(preferences.getLong(PREFS_PERIOD_OFFSET, 0), value)
            RewardPeriodType.CUSTOM -> RewardPeriod.CustomRange(
                getTimePeriod(PREFS_CUSTOM_START_TIME)!!,
                getTimePeriod(PREFS_CUSTOM_END_TIME)
            )
        }
    }

    private fun mapFromRewardPeriod(rewardPeriod: RewardPeriod): String {
        return rewardPeriod.type.toString()
    }
}
