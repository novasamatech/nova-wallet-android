package io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock

import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

data class GovernanceUnlockAffects(
    val transferableChange: Change<Balance>,
    val governanceLockChange: Change<Balance>,
    val claimableChunk: ClaimSchedule.UnlockChunk.Claimable?,
    val remainsLockedInfo:RemainsLockedInfo?
) {

    data class RemainsLockedInfo(
        val amount: Balance,
        val lockedInIds: List<String>
    )
}
