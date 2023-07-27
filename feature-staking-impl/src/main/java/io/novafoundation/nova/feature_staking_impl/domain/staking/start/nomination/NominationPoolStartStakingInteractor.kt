package io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NominationPoolStartStakingInteractor : StartStakingInteractor {

    override fun observeData(chain: Chain, asset: Asset): Flow<StartStakingData> {
        return flowOf(
            StartStakingData(
                availableBalance = 0.toBigInteger(),
                maxEarningRate = 0.toBigDecimal(),
                minStake = 0.toBigInteger(),
                payoutType = PayoutType.Manual,
                participationInGovernance = false
            )
        )
    }
}
