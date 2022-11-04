package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts

import io.novafoundation.nova.common.utils.anyIs
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegatedCollatorIds
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.redeemableIn
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegationState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.delegationStatesIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

interface ParachainStakingAlertsInteractor {

    fun alertsFlow(delegatorState: DelegatorState.Delegator): Flow<List<ParachainStakingAlert>>
}

private class AlertCalculationContext(
    val snapshots: AccountIdMap<CollatorSnapshot>,
    val delegatedCollatorsMetadata: AccountIdMap<CandidateMetadata>,
    val scheduledDelegationRequests: Collection<ScheduledDelegationRequest>,
    val currentRound: BigInteger,
    val delegatorState: DelegatorState.Delegator
)

private typealias AlertProducer = (AlertCalculationContext) -> List<ParachainStakingAlert>

class RealParachainStakingAlertsInteractor(
    private val candidatesRepository: CandidatesRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val delegatorStateRepository: DelegatorStateRepository,
) : ParachainStakingAlertsInteractor {

    override fun alertsFlow(delegatorState: DelegatorState.Delegator): Flow<List<ParachainStakingAlert>> {
        return flow {
            val chainId = delegatorState.chain.id
            val candidateMetadatas = candidatesRepository.getCandidatesMetadata(chainId, delegatorState.delegatedCollatorIds())

            val innerFlow = currentRoundRepository.currentRoundInfoFlow(chainId).flatMapLatest { currentRoundInfo ->
                val currentRound = currentRoundInfo.current
                val snapshots = currentRoundRepository.collatorsSnapshot(chainId, currentRound)

                delegatorStateRepository.scheduledDelegationRequestsFlow(delegatorState).map { scheduledDelegationRequests ->
                    val alertContext = AlertCalculationContext(
                        snapshots = snapshots,
                        delegatedCollatorsMetadata = candidateMetadatas,
                        scheduledDelegationRequests = scheduledDelegationRequests,
                        currentRound = currentRound,
                        delegatorState = delegatorState
                    )

                    alertProducers.flatMap { it.invoke(alertContext) }
                }
            }

            emitAll(innerFlow)
        }
    }

    private fun collatorsAlerts(context: AlertCalculationContext): List<ParachainStakingAlert> {
        val delegationStates = context.delegatorState
            .delegationStatesIn(context.snapshots, context.delegatedCollatorsMetadata)
            .values

        return listOfNotNull(
            ParachainStakingAlert.StakeMore.takeIf { delegationStates.anyIs(DelegationState.TOO_LOW_STAKE) },
            ParachainStakingAlert.ChangeCollator.takeIf { delegationStates.anyIs(DelegationState.COLLATOR_NOT_ACTIVE) }
        )
    }

    private fun redeemAlert(context: AlertCalculationContext): List<ParachainStakingAlert> {
        val redeemableRequests = context.scheduledDelegationRequests.filter { it.redeemableIn(context.currentRound) }

        return if (redeemableRequests.isNotEmpty()) {
            val amount = redeemableRequests.sumByBigInteger { it.action.amount }

            listOf(ParachainStakingAlert.RedeemTokens(amount))
        } else {
            emptyList()
        }
    }

    private val alertProducers: List<AlertProducer> = listOf(
        ::collatorsAlerts,
        ::redeemAlert
    )
}
