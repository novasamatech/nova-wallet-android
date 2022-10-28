package io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list

import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list.GovernanceLocksOverview.Lock
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class GovernanceLocksOverview(
    val totalLocked: Balance,
    val claimSchedule: List<Lock>,
) {

    sealed class Lock {

        class Claimable(val amount: Balance, val actions: List<ClaimSchedule.ClaimAction>) : Lock()

        class Pending(val amount: Balance, val timer: TimerValue) : Lock()
    }
}

fun GovernanceLocksOverview.canClaimTokens(): Boolean {
    return claimSchedule.any { it is Lock.Claimable }
}
