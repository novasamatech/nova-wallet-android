package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId

typealias StakingAccounts = Map<StakingOptionId, StakingOptionAccounts?>

data class StakingOptionAccounts(val rewards: AccountIdKey, val stakingStatus: AccountIdKey)
