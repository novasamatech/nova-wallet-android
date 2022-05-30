package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorBond
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.minimumStakeToGetRewards
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.isElected
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

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
            delegation.balance < delegatedCollator.minimumStakeToGetRewards -> DelegationState.TOO_LOW_STAKE
            else -> DelegationState.WAITING
        }
    }
}

fun DelegatorState.Delegator.delegationStatesIn(
    snapshots: AccountIdMap<CollatorSnapshot>,
    candidateMetadatas: AccountIdMap<CandidateMetadata>,
    systemForcedMinStake: BigInteger,
): Map<DelegatorBond, DelegationState> {
    return delegations.associateWith { delegation ->
        val collator = snapshots[delegation.owner.toHexString()]
        val candidateMetadata = candidateMetadatas.getValue(delegation.owner.toHexString())

        when {
            collator == null -> DelegationState.COLLATOR_NOT_ACTIVE
            collator.hasDelegator(accountId) -> DelegationState.ACTIVE
            delegation.balance < candidateMetadata.minimumStakeToGetRewards(systemForcedMinStake) -> DelegationState.TOO_LOW_STAKE
            else -> DelegationState.WAITING
        }
    }
}

private fun CollatorSnapshot.hasDelegator(delegatorId: AccountId): Boolean {
    return delegations.any { it.owner.contentEquals(delegatorId) }
}
