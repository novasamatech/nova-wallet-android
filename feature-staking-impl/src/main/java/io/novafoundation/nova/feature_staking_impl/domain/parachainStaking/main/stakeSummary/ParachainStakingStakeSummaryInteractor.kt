package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.stakeSummary

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.minimumStake
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import java.math.BigInteger

class ParachainStakingStakeSummaryInteractor(
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val roundDurationEstimator: RoundDurationEstimator,
) {

    private enum class DelegationState {
        COLLATOR_NOT_ACTIVE, TOO_LOW_STAKE, WAITING, ACTIVE
    }


    suspend fun delegatorStatusFlow(delegatorState: DelegatorState.Delegator): Flow<DelegatorStatus> {
        val chainId = delegatorState.chain.id
        val systemForcedMinStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val maxRewardedDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)

        return currentRoundRepository.currentRoundInfoFlow(chainId).mapLatest { currentRoundInfo ->
            val snapshots = currentRoundRepository.collatorsSnapshot(chainId, currentRoundInfo.current)

            val delegationStates = delegatorState.delegations.map { delegation ->
                val collator = snapshots[delegation.owner.toHexString()]

                when {
                    collator == null -> DelegationState.COLLATOR_NOT_ACTIVE
                    collator.hasDelegator(delegatorState.accountId) -> DelegationState.ACTIVE
                    delegation.balance < collator.minimumStake(systemForcedMinStake, maxRewardedDelegatorsPerCollator) -> DelegationState.TOO_LOW_STAKE
                    else -> DelegationState.WAITING
                }
            }

            when {
                delegationStates.anyIs(DelegationState.ACTIVE) -> DelegatorStatus.Active
                delegationStates.anyIs(DelegationState.WAITING) -> {
                    val targetRound = currentRoundInfo.current + BigInteger.ONE
                    val waitingDuration = roundDurationEstimator.timeTillRound(chainId, targetRound)

                    DelegatorStatus.Waiting(waitingDuration)
                }
                else -> DelegatorStatus.Inactive
            }
        }
    }

    private fun List<DelegationState>.anyIs(state: DelegationState) = any { it == state }

    private fun CollatorSnapshot.hasDelegator(delegatorId: AccountId): Boolean {
        return delegations.any { it.owner.contentEquals(delegatorId) }
    }
}
