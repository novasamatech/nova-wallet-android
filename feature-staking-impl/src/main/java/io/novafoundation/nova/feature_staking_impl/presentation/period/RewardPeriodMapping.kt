package io.novafoundation.nova.feature_staking_impl.presentation.period

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType.ALL_TIME
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType.WEEK
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType.MONTH
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType.QUARTER
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType.HALF_YEAR
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType.YEAR
import io.novafoundation.nova.feature_staking_impl.domain.period.getPeriodDays

fun mapRewardPeriodToString(resourceManager: ResourceManager, rewardPeriod: RewardPeriod): String {
    return when (rewardPeriod.type) {
        ALL_TIME -> resourceManager.getString(R.string.staking_period_all_short)
        WEEK -> resourceManager.getString(R.string.staking_period_week_short)
        MONTH -> resourceManager.getString(R.string.staking_period_month_short)
        QUARTER -> resourceManager.getString(R.string.staking_period_quarter_short)
        HALF_YEAR -> resourceManager.getString(R.string.staking_period_half_year_short)
        YEAR -> resourceManager.getString(R.string.staking_period_year_short)
        else -> {
            resourceManager.getString(R.string.staking_period_custom_short, rewardPeriod.getPeriodDays())
        }
    }
}
