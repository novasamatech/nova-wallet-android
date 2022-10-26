package io.novafoundation.nova.feature_governance_api.domain.locks

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.UnlockChunk
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

@JvmInline
value class ClaimSchedule(val chunks: List<UnlockChunk>) {

    sealed class UnlockChunk {

        data class Claimable(val amount: Balance, val actions: List<ClaimAction>) : UnlockChunk()

        data class Pending(val amount: Balance, val claimableAt: BlockNumber) : UnlockChunk()
    }

    sealed class ClaimAction {

        data class Unlock(val trackId: TrackId) : ClaimAction()

        data class RemoveVote(val trackId: TrackId, val referendumId: ReferendumId) : ClaimAction()
    }
}

fun ClaimSchedule.claimableChunk(): UnlockChunk.Claimable? {
    return chunks.first().castOrNull<UnlockChunk.Claimable>()
}
