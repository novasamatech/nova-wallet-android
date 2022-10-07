package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

enum class ReferendumGroup {
    ONGOING, COMPLETED
}

data class ReferendumPreview(
    val id: ReferendumId,
    val status: ReferendumStatus,
    val offChainMetadata: OffChainMetadata?,
    val onChainMetadata: OnChainMetadata?,
    val track: Track?,
    val voting: ReferendumVoting?,
    val userVote: AccountVote?
) {

    data class Track(val name: String)

    data class OffChainMetadata(val title: String)

    data class OnChainMetadata(val proposalHash: String)
}

data class ReferendumVoting(
    val support: Support,
    val approval: Approval
) {

    data class Support(
        val threshold: Balance,
        val turnout: Balance
    )

    data class Approval(
        val ayeFraction: Perbill,
        val nayFraction: Perbill,
        val threshold: Perbill
    )
}

fun ReferendumVoting.Support.passes(): Boolean {
    return turnout > threshold
}

sealed class ReferendumStatus {

    data class Preparing(val reason: PreparingReason) : ReferendumStatus()

    data class InQueue(val timeOutIn: TimerValue) : ReferendumStatus()

    data class Deciding(val rejectIn: TimerValue) : ReferendumStatus()

    data class Confirming(val approveIn: TimerValue) : ReferendumStatus()

    data class Approved(val executeIn: TimerValue) : ReferendumStatus()

    object Executed : ReferendumStatus()

    sealed class NotExecuted : ReferendumStatus() {

        object TimedOut : NotExecuted()

        object Killed : NotExecuted()

        object Cancelled : NotExecuted()

        object Rejected : NotExecuted()
    }
}

sealed class PreparingReason {

    object WaitingForDeposit : PreparingReason()

    data class DecidingIn(val timeLeft: TimerValue) : PreparingReason()
}
