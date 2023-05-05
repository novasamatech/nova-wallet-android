package io.novafoundation.nova.feature_staking_impl.presentation.period

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.getPeriodMillis
import java.util.concurrent.TimeUnit

fun mapRewardPeriodToString(resourceManager: ResourceManager, rewardPeriod: RewardPeriod): String {
    return when (rewardPeriod) {
        is RewardPeriod.All -> resourceManager.getString(R.string.staking_period_all_short)
        is RewardPeriod.Week -> resourceManager.getString(R.string.staking_period_week_short)
        is RewardPeriod.Month -> resourceManager.getString(R.string.staking_period_month_short)
        is RewardPeriod.Quarter -> resourceManager.getString(R.string.staking_period_quarter_short)
        is RewardPeriod.HalfYear -> resourceManager.getString(R.string.staking_period_half_year_short)
        is RewardPeriod.Year -> resourceManager.getString(R.string.staking_period_year_short)
        is RewardPeriod.Custom -> {
            val days = TimeUnit.MILLISECONDS.toDays(rewardPeriod.getPeriodMillis())
            resourceManager.getString(R.string.staking_period_custom_short, days)
        }
    }
}
