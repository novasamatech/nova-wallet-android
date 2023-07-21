package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.BondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolPoints
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class BondedPoolState(
    val bondedPool: BondedPool,
    override val poolBalance: Balance
) : PoolBalanceConvertable {

    override val poolPoints: PoolPoints = bondedPool.points
}
