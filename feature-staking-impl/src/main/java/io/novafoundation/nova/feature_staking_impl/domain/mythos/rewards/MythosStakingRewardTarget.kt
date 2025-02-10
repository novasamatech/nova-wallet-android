package io.novafoundation.nova.feature_staking_impl.domain.mythos.rewards

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class MythosStakingRewardTarget(
    val totalStake: Balance,
    val accountId: AccountIdKey
)
