package io.novafoundation.nova.feature_staking_impl.presentation.period

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType
import io.novafoundation.nova.feature_staking_impl.domain.period.getPeriodDays

fun mapRewardPeriodToString(resourceManager: ResourceManager, rewardPeriod: RewardPeriod): String {
    return when (rewardPeriod.type) {
        RewardPeriodType.AllTime -> resourceManager.getString(R.string.staking_period_all_short)
        RewardPeriodType.Preset.WEEK -> resourceManager.getString(R.string.staking_period_week_short)
        RewardPeriodType.Preset.MONTH -> resourceManager.getString(R.string.staking_period_month_short)
        RewardPeriodType.Preset.QUARTER -> resourceManager.getString(R.string.staking_period_quarter_short)
        RewardPeriodType.Preset.HALF_YEAR -> resourceManager.getString(R.string.staking_period_half_year_short)
        RewardPeriodType.Preset.YEAR -> resourceManager.getString(R.string.staking_period_year_short)
        RewardPeriodType.Custom -> {
            resourceManager.getString(R.string.staking_period_custom_short, rewardPeriod.getPeriodDays())
        }
    }
}
