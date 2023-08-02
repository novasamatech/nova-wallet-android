package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.StartStakingEraInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class RelaychainStakingEraInteractor(
    private val stakingInteractor: StakingInteractor
) : StakingEraInteractor {

    override fun observeEraInfo(chain: Chain): Flow<StartStakingEraInfo> {
        return flowOf {
            val eraDuration = stakingInteractor.getEraDuration()
            StartStakingEraInfo(
                unstakeTime = stakingInteractor.getLockupDuration(),
                eraDuration = eraDuration,
                firstRewardReceivingDuration = stakingInteractor.getFirstRewardReceivingDelay()
            )
        }
    }
}
