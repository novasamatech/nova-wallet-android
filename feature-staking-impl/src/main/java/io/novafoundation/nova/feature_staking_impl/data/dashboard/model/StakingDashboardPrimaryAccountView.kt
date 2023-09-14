package io.novafoundation.nova.feature_staking_impl.data.dashboard.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId

data class StakingDashboardOptionAccounts(
    val stakingOptionId: StakingOptionId,
    val stakingStatusAccount: AccountIdKey?,
    val rewardsAccount: AccountIdKey?
) : Identifiable {

    override val identifier: String = "${stakingOptionId.chainId}:${stakingOptionId.chainAssetId}:${stakingOptionId.stakingType}"
}
