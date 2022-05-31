package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.SelectedCollator
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import java.math.BigInteger

interface CollatorsUseCase {

    suspend fun getSelectedCollators(delegatorState: DelegatorState): List<SelectedCollator>

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

    override suspend fun getSelectedCollators(delegatorState: DelegatorState): List<SelectedCollator> {
        return when (delegatorState) {
            is DelegatorState.Delegator -> {
                val delegationAmountByCollator = delegatorState.delegations.associateBy(
                    keySelector = { it.owner.toHexString() },
                    valueTransform = { it.balance }
                )
                val stakedCollatorsIds = delegationAmountByCollator.keys

                val collatorSource = CollatorProvider.CollatorSource.Custom(stakedCollatorsIds)

                collatorProvider.getCollators(delegatorState.chain.id, collatorSource)
                    .map { collator ->
                        SelectedCollator(
                            collator = collator,
                            delegation = delegationAmountByCollator.getValue(collator.accountIdHex)
                        )
                    }
                    .sortedByDescending(SelectedCollator::delegation)
            }

            is DelegatorState.None -> emptyList()
        }
    }
}
