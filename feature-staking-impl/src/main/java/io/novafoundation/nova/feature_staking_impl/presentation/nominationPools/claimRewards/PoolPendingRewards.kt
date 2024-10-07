package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class PoolPendingRewards(
    val amount: Balance,
    val poolMember: PoolMember,
)
