package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

enum class ReferendumGroup {
    ONGOING, COMPLETED
}

data class ReferendumPreview(
    val id: ReferendumId,
    val status: ReferendumStatus,
    val offChainMetadata: OffChainMetadata?,
    val onChainMetadata: OnChainMetadata?,
    val track: ReferendumTrack?,
    val voting: ReferendumVoting?,
    val userVote: AccountVote?
) {


    data class OffChainMetadata(val title: String)

    data class OnChainMetadata(val proposal: ReferendumProposal)
}

sealed class ReferendumProposal {

    class Hash(val callHash: String): ReferendumProposal()

    class Call(val call: GenericCall.Instance): ReferendumProposal()
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
