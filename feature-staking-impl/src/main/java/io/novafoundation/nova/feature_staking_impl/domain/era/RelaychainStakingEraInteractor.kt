package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.StartStakingEraInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.milliseconds

class RelaychainStakingEraInteractor(
    private val stakingInteractor: StakingInteractor
) : StakingEraInteractor {

    override fun observeEraInfo(chain: Chain): Flow<StartStakingEraInfo> {
        return flowOf {
            StartStakingEraInfo(
                remainingEraTime = stakingInteractor.getRemainingEraTime().toLong().milliseconds,
                unstakeTime = stakingInteractor.getLockupDuration(),
                eraDuration = stakingInteractor.getEraDuration()
            )
        }
    }
}
