package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common

import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorBond
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.minimumStake
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

enum class DelegationState {
    COLLATOR_NOT_ACTIVE, TOO_LOW_STAKE, WAITING, ACTIVE
}

fun DelegatorState.Delegator.delegationStatesIn(
    snapshots: AccountIdMap<CollatorSnapshot>,
    systemForcedMinStake: BigInteger,
    maxRewardedDelegatorsPerCollator: BigInteger,
): Map<DelegatorBond, DelegationState> {
    return delegations.associateWith { delegation ->
        val collator = snapshots[delegation.owner.toHexString()]

        when {
            collator == null -> DelegationState.COLLATOR_NOT_ACTIVE
            collator.hasDelegator(accountId) -> DelegationState.ACTIVE
            delegation.balance < collator.minimumStake(systemForcedMinStake, maxRewardedDelegatorsPerCollator) -> DelegationState.TOO_LOW_STAKE
            else -> DelegationState.WAITING
        }
    }
}

private fun CollatorSnapshot.hasDelegator(delegatorId: AccountId): Boolean {
    return delegations.any { it.owner.contentEquals(delegatorId) }
}
