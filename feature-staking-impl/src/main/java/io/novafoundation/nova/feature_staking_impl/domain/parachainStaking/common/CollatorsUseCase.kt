package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import java.math.BigInteger

interface CollatorsUseCase {

    suspend fun getSelectedCollators(delegatorState: DelegatorState): List<Collator>

    suspend fun maxRewardedDelegatorsPerCollator(): Int

    suspend fun defaultMinimumStake(): BigInteger
}

class RealCollatorsUseCase(
    private val singleAssetSharedState: SingleAssetSharedState,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val collatorProvider: CollatorProvider,
) : CollatorsUseCase {

    override suspend fun maxRewardedDelegatorsPerCollator(): Int {
        val chainId = singleAssetSharedState.chainId()

        return parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId).toInt()
    }

    override suspend fun defaultMinimumStake(): BigInteger {
        return parachainStakingConstantsRepository.systemForcedMinStake(singleAssetSharedState.chainId())
    }

    override suspend fun getSelectedCollators(delegatorState: DelegatorState): List<Collator> {
        return when (delegatorState) {
            is DelegatorState.Delegator -> {
                val stakedCollatorsIds = delegatorState.delegations.map { it.owner.toHexString() }

                val collatorSource = CollatorProvider.CollatorSource.Custom(stakedCollatorsIds)
                collatorProvider.getCollators(delegatorState.chain.id, collatorSource)
            }
            is DelegatorState.None -> emptyList()
        }
    }
}
