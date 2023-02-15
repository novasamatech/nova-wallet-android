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

        data class Pending(val amount: Balance, val claimableAt: ClaimTime) : UnlockChunk()
    }

    sealed class ClaimAction {

        data class Unlock(val trackId: TrackId) : ClaimAction()

        data class RemoveVote(val trackId: TrackId, val referendumId: ReferendumId) : ClaimAction()
    }
}

sealed class ClaimTime : Comparable<ClaimTime> {

    object UntilAction : ClaimTime()

    data class At(val block: BlockNumber) : ClaimTime()

    override operator fun compareTo(other: ClaimTime): Int {
        return when {
            this is At && other is At -> block.compareTo(other.block)
            this is UntilAction && other is UntilAction -> 0
            this is UntilAction -> 1
            else -> -1
        }
    }
}

fun ClaimSchedule.claimableChunk(): UnlockChunk.Claimable? {
    return chunks.firstOrNull().castOrNull<UnlockChunk.Claimable>()
}

fun ClaimSchedule.hasClaimableLocks(): Boolean {
    return claimableChunk() != null
}
