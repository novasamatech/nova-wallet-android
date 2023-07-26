package io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.BaseStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NominationPoolStartStakingInteractor(
    stakingType: Chain.Asset.StakingType,
    accountRepository: AccountRepository,
    walletRepository: WalletRepository,
    coroutineScope: CoroutineScope,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
) : BaseStartStakingInteractor(
    stakingType,
    accountRepository,
    walletRepository,
    coroutineScope
) {

    override fun observeData(chain: Chain, asset: Asset): Flow<StartStakingData> {
        return nominationPoolGlobalsRepository.observeMinJoinBond(chain.id)
            .map { minJoinBond ->
                StartStakingData(
                    availableBalance = asset.freeInPlanks,
                    maxEarningRate = 0.toBigDecimal(), // TODO: not implemented yet
                    minStake = minJoinBond,
                    payoutType = PayoutType.Manual,
                    participationInGovernance = false
                )
            }
    }
}
