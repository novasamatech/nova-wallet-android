package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.unbondings

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ParachainStakingUnbondingsInteractor(
    private val delegatorStateRepository: DelegatorStateRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val roundDurationEstimator: RoundDurationEstimator,
) {

    fun unbondingsFlow(delegatorState: DelegatorState.Delegator): Flow<Unbondings> = flow {
        val chainId = delegatorState.chain.id
        val scheduledRequests = delegatorStateRepository.scheduledDelegationRequests(delegatorState)

        val unbondingsFlow = currentRoundRepository.currentRoundInfoFlow(chainId).map { currentRoundInfo ->
            val currentRoundIndex = currentRoundInfo.current
            val durationCalculator = roundDurationEstimator.createDurationCalculator(chainId)

            val unbondingsList = scheduledRequests.map { scheduledDelegationRequest ->
                val status = if (scheduledDelegationRequest.isRedeemable(currentRoundIndex)) {
                    Unbonding.Status.Redeemable
                } else {
                    val calculatedDuration = durationCalculator.timeTillRound(scheduledDelegationRequest.whenExecutable)

                    Unbonding.Status.Unbonding(
                        timeLeft = calculatedDuration.duration.toLongMilliseconds(),
                        calculatedAt = calculatedDuration.calculatedAt
                    )
                }

                Unbonding(
                    amount = scheduledDelegationRequest.action.amount,
                    status = status
                )
            }

            Unbondings.from(unbondingsList)
        }

        emitAll(unbondingsFlow)
    }

    private fun ScheduledDelegationRequest.isRedeemable(currentRoundIndex: RoundIndex): Boolean {
        return whenExecutable <= currentRoundIndex
    }
}
