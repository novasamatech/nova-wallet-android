package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegatedCollatorIdsHex
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider.CollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegationState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.delegationStatesFor
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest

interface CurrentCollatorInteractor {

    fun currentCollatorsFlow(delegatorState: DelegatorState.Delegator): Flow<GroupedList<DelegatedCollatorGroup, DelegatedCollator>>
}

class RealCurrentCollatorInteractor(
    private val currentRoundRepository: CurrentRoundRepository,
    private val collatorProvider: CollatorProvider,
    private val stakingSharedState: StakingSharedState,
) : CurrentCollatorInteractor {

    override fun currentCollatorsFlow(delegatorState: DelegatorState.Delegator): Flow<GroupedList<DelegatedCollatorGroup, DelegatedCollator>> = flow {
        val chainId = delegatorState.chain.id
        val stakingOption = stakingSharedState.selectedOption()

        val innerFlow = currentRoundRepository.currentRoundInfoFlow(chainId).mapLatest { currentRoundInfo ->
            val snapshots = currentRoundRepository.collatorsSnapshot(chainId, currentRoundInfo.current)

            val delegationAccountIds = delegatorState.delegatedCollatorIdsHex()
            val collatorsById = collatorProvider.getCollators(stakingOption, CollatorSource.Custom(delegationAccountIds), snapshots)
                .associateBy { it.accountIdHex }

            val delegationStates = delegatorState.delegationStatesFor(collatorsById)

            val delegatedCollators = delegatorState.delegations.map { delegation ->
                val delegationState = delegationStates.getValue(delegation)

                DelegatedCollator(
                    collator = collatorsById.getValue(delegation.owner.toHexString()),
                    delegation = delegation.balance,
                    delegationStatus = delegationState
                )
            }

            groupDelegatedCollators(delegatedCollators)
        }

        emitAll(innerFlow)
    }

    private fun groupDelegatedCollators(delegatedCollators: List<DelegatedCollator>): Map<DelegatedCollatorGroup, List<DelegatedCollator>> {
        val delegatorsByStatus = delegatedCollators.groupBy { it.delegationStatus }

        val electedCollatorsCount = delegatorsByStatus.sizeOf(DelegationState.ACTIVE) + delegatorsByStatus.sizeOf(DelegationState.TOO_LOW_STAKE)
        val activeGroup = DelegatedCollatorGroup.Active(electedCollatorsCount)

        return delegatorsByStatus.mapKeys { (status, groupCollators) ->
            val groupSize = groupCollators.size

            when (status) {
                DelegationState.ACTIVE -> activeGroup
                DelegationState.TOO_LOW_STAKE -> DelegatedCollatorGroup.Elected(groupSize)
                DelegationState.COLLATOR_NOT_ACTIVE -> DelegatedCollatorGroup.Inactive(groupSize)
                DelegationState.WAITING -> DelegatedCollatorGroup.WaitingForNextEra(groupSize)
            }
        }.toSortedMap(DelegatedCollatorGroup.COMPARATOR)
    }

    private fun <K> Map<K, List<*>>.sizeOf(key: K) = get(key).orEmpty().size
}
