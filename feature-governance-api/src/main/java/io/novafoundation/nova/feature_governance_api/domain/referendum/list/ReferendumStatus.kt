package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue

sealed class ReferendumStatus {

    sealed class Ongoing : ReferendumStatus() {

        data class Preparing(val reason: PreparingReason, val timeOutIn: TimerValue) : Ongoing()

        data class InQueue(val timeOutIn: TimerValue, val position: TrackQueue.Position) : Ongoing()

        data class Deciding(val rejectIn: TimerValue) : Ongoing()

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
