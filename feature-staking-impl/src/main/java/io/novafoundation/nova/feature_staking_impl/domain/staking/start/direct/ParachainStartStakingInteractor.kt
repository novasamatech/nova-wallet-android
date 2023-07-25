package io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.rewards.DAYS_IN_YEAR
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.BaseStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParachainStartStakingInteractor(
    stakingType: Chain.Asset.StakingType,
    coroutineScope: CoroutineScope,
    accountRepository: AccountRepository,
    walletRepository: WalletRepository,
    private val parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
    private val parachainStakingRewardCalculator: ParachainStakingRewardCalculator
) : BaseStartStakingInteractor(stakingType, accountRepository, walletRepository, coroutineScope) {

    override fun observeData(chain: Chain, asset: Asset): Flow<StartStakingData> {
        return parachainNetworkInfoInteractor.observeRoundInfo(chain.id).map { activeEraInfo ->
            StartStakingData(
                availableBalance = getAvailableBalance(asset),
                maxEarningRate = parachainStakingRewardCalculator.maximumGain(DAYS_IN_YEAR),
                minStake = activeEraInfo.minimumStake,
                payoutType = getPayoutType(chain),
                participationInGovernance = chain.governance.isNotEmpty()
            )
        }
    }

    private fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.freeInPlanks
    }

    private fun getPayoutType(chain: Chain): PayoutType {
        return when (chain.id) {
            ChainGeneses.MOONBEAM,
            ChainGeneses.MOONRIVER -> PayoutType.Automatic.Payout
            else -> PayoutType.Manual
        }
    }
}
