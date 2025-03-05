package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.NominationPoolsAvailableBalanceResolver.MaxBalanceToStake
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

interface NominationPoolsAvailableBalanceResolver {

    suspend fun availableBalanceToStartStaking(asset: Asset): Balance

    suspend fun maximumBalanceToStake(asset: Asset, fee: Balance): MaxBalanceToStake

    suspend fun maximumBalanceToStake(asset: Asset): Balance

    class MaxBalanceToStake(val maxToStake: Balance, val existentialDeposit: Balance)
}

class RealNominationPoolsAvailableBalanceResolver(
    private val walletConstants: WalletConstants
) : NominationPoolsAvailableBalanceResolver {

    override suspend fun availableBalanceToStartStaking(asset: Asset): Balance {
        return asset.transferableInPlanks
    }

    override suspend fun maximumBalanceToStake(asset: Asset, fee: Balance): MaxBalanceToStake {
        val existentialDeposit = walletConstants.existentialDeposit(asset.token.configuration.chainId)

        val maxToStake = minOf(asset.transferableInPlanks, asset.balanceCountedTowardsEDInPlanks - existentialDeposit) - fee

        return MaxBalanceToStake(maxToStake.atLeastZero(), existentialDeposit)
    }

    override suspend fun maximumBalanceToStake(asset: Asset): Balance {
        val existentialDeposit = walletConstants.existentialDeposit(asset.token.configuration.chainId)
        return minOf(asset.transferableInPlanks, asset.balanceCountedTowardsEDInPlanks - existentialDeposit)
    }
}
