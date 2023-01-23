package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import jp.co.soramitsu.fearless_utils.runtime.AccountId
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
    val userVote: ReferendumVote?
) {

    data class OffChainMetadata(val title: String)

    data class OnChainMetadata(val proposal: ReferendumProposal)
}

sealed class ReferendumVote(val vote: AccountVote) {

    class User(vote: AccountVote): ReferendumVote(vote)

    class Account(val who: AccountId, val whoIdentity: Identity?, vote: AccountVote): ReferendumVote(vote)
}

sealed class ReferendumProposal {

    class Hash(val callHash: String) : ReferendumProposal()

    class Call(val call: GenericCall.Instance) : ReferendumProposal()
}

sealed class ReferendumStatus {

    sealed class Ongoing : ReferendumStatus() {

        data class Preparing(val reason: PreparingReason, val timeOutIn: TimerValue) : Ongoing()

        data class InQueue(val timeOutIn: TimerValue, val position: TrackQueue.Position) : Ongoing()

        data class Rejecting(val rejectIn: TimerValue) : Ongoing()

        data class Confirming(val approveIn: TimerValue) : Ongoing()
    }

    data class Approved(val since: BlockNumber, val executeIn: TimerValue) : ReferendumStatus()

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
