package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.BondedPoolState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class PoolMemberState(
    val bondedPoolState: BondedPoolState,
    val poolMember: PoolMember,
)

val PoolMemberState.stakedBalance: Balance
    get() = bondedPoolState.amountOf(poolMember.points)
