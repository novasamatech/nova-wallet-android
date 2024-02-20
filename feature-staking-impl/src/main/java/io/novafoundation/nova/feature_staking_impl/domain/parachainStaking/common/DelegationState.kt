package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorBond
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isStakeEnoughToEarnRewards
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.isElected
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId

enum class DelegationState {
    COLLATOR_NOT_ACTIVE, TOO_LOW_STAKE, WAITING, ACTIVE
}

fun DelegatorState.Delegator.delegationStatesFor(
    delegatedCollators: AccountIdMap<Collator>,
): Map<DelegatorBond, DelegationState> {
    return delegations.associateWith { delegation ->
        val delegatedCollator = delegatedCollators.getValue(delegation.owner.toHexString())

        when {
            !delegatedCollator.isElected -> DelegationState.COLLATOR_NOT_ACTIVE
            delegatedCollator.snapshot!!.hasDelegator(accountId) -> DelegationState.ACTIVE
            !delegatedCollator.candidateMetadata.isStakeEnoughToEarnRewards(delegation.balance) -> DelegationState.TOO_LOW_STAKE
            else -> DelegationState.WAITING
        }
    }
}

fun DelegatorState.Delegator.delegationStatesIn(
    snapshots: AccountIdMap<CollatorSnapshot>,
    candidateMetadatas: AccountIdMap<CandidateMetadata>,
): Map<DelegatorBond, DelegationState> {
    return delegations.associateWith { delegation ->
        val collator = snapshots[delegation.owner.toHexString()]
        val candidateMetadata = candidateMetadatas.getValue(delegation.owner.toHexString())

        when {
            collator == null -> DelegationState.COLLATOR_NOT_ACTIVE
            collator.hasDelegator(accountId) -> DelegationState.ACTIVE
            !candidateMetadata.isStakeEnoughToEarnRewards(delegation.balance) -> DelegationState.TOO_LOW_STAKE
            else -> DelegationState.WAITING
        }
    }
}

private fun CollatorSnapshot.hasDelegator(delegatorId: AccountId): Boolean {
    return delegations.any { it.owner.contentEquals(delegatorId) }
}
