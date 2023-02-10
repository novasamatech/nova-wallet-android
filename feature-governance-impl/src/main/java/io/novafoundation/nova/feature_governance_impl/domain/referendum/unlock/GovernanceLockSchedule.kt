package io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock

import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_api.domain.locks.hasClaimableLocks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class GovernanceLocksOverview(
    val totalLocked: Balance,
    val locks: List<Lock>,
    val claimSchedule: ClaimSchedule,
) {

    sealed class Lock {

        class Claimable(val amount: Balance, val actions: List<ClaimSchedule.ClaimAction>) : Lock()

        class Pending(val amount: Balance, val claimTime: ClaimTime) : Lock()
    }

    sealed class ClaimTime {

        class At(val timer: TimerValue) : ClaimTime()

        object UntilAction : ClaimTime()
    }
}

fun GovernanceLocksOverview.canClaimTokens(): Boolean {
    return claimSchedule.hasClaimableLocks()
}
