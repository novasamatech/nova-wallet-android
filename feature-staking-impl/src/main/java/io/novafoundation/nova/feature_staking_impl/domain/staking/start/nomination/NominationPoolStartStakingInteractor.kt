package io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class NominationPoolStartStakingInteractor(
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
    private val stakingOption: StakingOption,
) : StartStakingInteractor {

    override fun observeData(): Flow<StartStakingData> {
        return nominationPoolGlobalsRepository.observeMinJoinBond(stakingOption.chain.id)
            .map { minJoinBond ->
                StartStakingData(
                    maxEarningRate = 0.toBigDecimal(), // TODO: not implemented yet
                    minStake = minJoinBond,
                    payoutType = PayoutType.Manual,
                    participationInGovernance = false
                )
            }
    }

    override fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.transferableInPlanks
    }
}
