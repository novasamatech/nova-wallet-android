package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.stakeSummary

import io.novafoundation.nova.common.utils.anyIs
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.activeBonded
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegatedCollatorIds
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegationState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.delegationStatesIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import java.math.BigInteger

class ParachainStakingStakeSummaryInteractor(
    private val currentRoundRepository: CurrentRoundRepository,
    private val candidatesRepository: CandidatesRepository,
    private val roundDurationEstimator: RoundDurationEstimator,
) {

    suspend fun delegatorStatusFlow(delegatorState: DelegatorState.Delegator): Flow<DelegatorStatus> {
        val chainId = delegatorState.chain.id

        return currentRoundRepository.currentRoundInfoFlow(chainId).transformLatest { currentRoundInfo ->
            val snapshots = currentRoundRepository.collatorsSnapshot(chainId, currentRoundInfo.current)
            val delegatedIds = delegatorState.delegatedCollatorIds()
            val candidateMetadatas = candidatesRepository.getCandidatesMetadata(chainId, delegatedIds)

            val delegationStates = delegatorState.delegationStatesIn(snapshots, candidateMetadatas).values

            when {
                delegatorState.activeBonded.isZero -> emit(DelegatorStatus.Inactive)
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
