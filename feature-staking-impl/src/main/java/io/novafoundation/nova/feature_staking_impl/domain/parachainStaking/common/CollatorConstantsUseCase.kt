package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState

interface CollatorConstantsUseCase {

    suspend fun maxRewardedDelegatorsPerCollator(): Int
}

class RealCollatorConstantsUseCase(
    private val singleAssetSharedState: SingleAssetSharedState,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
) : CollatorConstantsUseCase {

    override suspend fun maxRewardedDelegatorsPerCollator(): Int {
        val chainId = singleAssetSharedState.chainId()

        return parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId).toInt()
    }
}
