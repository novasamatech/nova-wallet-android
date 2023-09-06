package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

interface NominationPoolsAvailableBalanceResolver {

    suspend fun availableBalanceToStartStaking(asset: Asset): Balance
}

class RealNominationPoolsAvailableBalanceResolver : NominationPoolsAvailableBalanceResolver {

    override suspend fun availableBalanceToStartStaking(asset: Asset): Balance {
        return asset.transferableInPlanks
    }
}
