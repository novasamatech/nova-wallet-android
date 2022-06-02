package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.unbondings

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Identity
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.ScheduledDelegationRequest
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.redeemableIn
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.unbondingIn
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.from
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

class DelegationRequestWithCollatorInfo(
    val request: ScheduledDelegationRequest,
    val collatorIdentity: Identity?,
) : Identifiable {

    override val identifier by lazy { request.collator.toHexString() }
}

@OptIn(ExperimentalTime::class)
class ParachainStakingUnbondingsInteractor(
    private val delegatorStateRepository: DelegatorStateRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val roundDurationEstimator: RoundDurationEstimator,
    private val identityRepository: IdentityRepository,
) {

    suspend fun pendingUnbondings(delegatorState: DelegatorState.Delegator): List<DelegationRequestWithCollatorInfo> = withContext(Dispatchers.Default) {
        val requests = delegatorStateRepository.scheduledDelegationRequests(delegatorState)
        val currentRound = currentRoundRepository.currentRoundInfo(delegatorState.chain.id).current

        val unbondingRequests = requests.values.filter { it.unbondingIn(currentRound) }

        val collatorIds = unbondingRequests.map { it.collator.toHexString() }
        val identities = identityRepository.getIdentitiesFromIds(delegatorState.chain.id, collatorIds)

        collatorIds.map {
            DelegationRequestWithCollatorInfo(
                request = requests.getValue(it),
                collatorIdentity = identities[it]
            )
        }
    }

    fun unbondingsFlow(delegatorState: DelegatorState.Delegator): Flow<Unbondings> = flow {
        val chainId = delegatorState.chain.id
        val scheduledRequests = delegatorStateRepository.scheduledDelegationRequests(delegatorState).values

        val unbondingsFlow = currentRoundRepository.currentRoundInfoFlow(chainId).map { currentRoundInfo ->
            val currentRoundIndex = currentRoundInfo.current
            val durationCalculator = roundDurationEstimator.createDurationCalculator(chainId)

            val unbondingsList = scheduledRequests.map { scheduledDelegationRequest ->
                val status = if (scheduledDelegationRequest.redeemableIn(currentRoundIndex)) {
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
}
