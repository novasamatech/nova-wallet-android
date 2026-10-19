package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake.StakingStatus
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel

internal fun mapDashboardStakingStatus(status: StakingStatus, resources: ResourceManager): StakeStatusModel = when (status) {
    StakingStatus.ACTIVE -> StakeStatusModel(
        R.drawable.ic_indicator_positive_pulse,
        resources.getString(R.string.common_active),
        R.color.text_positive
    )
    StakingStatus.INACTIVE -> StakeStatusModel(
        R.drawable.ic_indicator_negative_pulse,
        resources.getString(R.string.staking_nominator_status_inactive),
        R.color.text_negative
    )
    StakingStatus.WAITING -> StakeStatusModel(
        R.drawable.ic_indicator_inactive_pulse,
        resources.getString(R.string.common_waiting),
        R.color.text_primary
    )
}
