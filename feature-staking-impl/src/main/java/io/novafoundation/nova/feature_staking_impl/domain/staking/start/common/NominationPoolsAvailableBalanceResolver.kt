package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

interface NominationPoolsAvailableBalanceResolver {

    suspend fun availableBalanceToStartStaking(asset: Asset): Balance
}

class RealNominationPoolsAvailableBalanceResolver(
    private val walletConstants: WalletConstants
) : NominationPoolsAvailableBalanceResolver {

    override suspend fun availableBalanceToStartStaking(asset: Asset): Balance {
        val existentialDeposit = walletConstants.existentialDeposit(asset.token.configuration.chainId)

        val availableBalance = minOf(asset.transferableInPlanks, asset.totalInPlanks - existentialDeposit)

        return availableBalance.atLeastZero()
    }
}
