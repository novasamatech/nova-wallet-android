package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.stakeSummary

import io.novafoundation.nova.common.utils.anyIs
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegationState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.delegationStatesIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import java.math.BigInteger

class ParachainStakingStakeSummaryInteractor(
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val roundDurationEstimator: RoundDurationEstimator,
) {

    suspend fun delegatorStatusFlow(delegatorState: DelegatorState.Delegator): Flow<DelegatorStatus> {
        val chainId = delegatorState.chain.id
        val systemForcedMinStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val maxRewardedDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)

        return currentRoundRepository.currentRoundInfoFlow(chainId).transformLatest { currentRoundInfo ->
            val snapshots = currentRoundRepository.collatorsSnapshot(chainId, currentRoundInfo.current)

            val delegationStates = delegatorState.delegationStatesIn(snapshots, systemForcedMinStake, maxRewardedDelegatorsPerCollator).values

            when {
                delegationStates.anyIs(DelegationState.ACTIVE) -> emit(DelegatorStatus.Active)
                delegationStates.anyIs(DelegationState.WAITING) -> {
                    val targetRound = currentRoundInfo.current + BigInteger.ONE

                    val waitingStatusFlow = roundDurationEstimator.timeTillRoundFlow(chainId, targetRound)
                        .map(DelegatorStatus::Waiting)

                    emitAll(waitingStatusFlow)
                }
                else -> emit(DelegatorStatus.Inactive)
            }
        }
    }
}
