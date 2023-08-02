package io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class NominationPoolStartStakingInteractor(
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
) : StartStakingInteractor {

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
