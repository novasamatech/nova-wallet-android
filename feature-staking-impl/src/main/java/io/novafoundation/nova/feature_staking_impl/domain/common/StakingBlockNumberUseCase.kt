package io.novafoundation.nova.feature_staking_impl.domain.common

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface StakingBlockNumberUseCase {

    fun currentBlockNumberFlow(): Flow<BlockNumber>
}

@FeatureScope
class RealStakingBlockNumberUseCase @Inject constructor(
    private val chainStateRepository: ChainStateRepository,
    private val stakingSharedState: StakingSharedState,
) : StakingBlockNumberUseCase {

    override fun currentBlockNumberFlow(): Flow<BlockNumber> {
        return flowOfAll {
            val chain = stakingSharedState.chainId()

            chainStateRepository.currentBlockNumberFlow(chain)
        }
    }
}
