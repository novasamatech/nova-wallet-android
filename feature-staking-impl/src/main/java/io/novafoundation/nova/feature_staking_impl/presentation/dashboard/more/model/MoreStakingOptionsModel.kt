package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.NoStakeItem

class MoreStakingOptionsModel(
    val inAppStaking: List<NoStakeItem>,
    val browserStaking: ExtendedLoadingState<List<StakingDAppModel>>
)
