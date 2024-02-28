package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue

enum class ReferendumStatusType(val id: String) {
    WAITING_DEPOSIT("WaitingForDeposit"),
    PREPARING("Preparing"),
    IN_QUEUE("InQueue"),
    DECIDING("Deciding"),
    CONFIRMING("Confirming"),
    APPROVED("Approved"),
    EXECUTED("Executed"),
    TIMED_OUT("TimedOut"),
    KILLED("Killed"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected")
}

sealed class ReferendumStatus {

    abstract val type: ReferendumStatusType

    sealed class Ongoing : ReferendumStatus() {
        data class Preparing(val reason: PreparingReason, val timeOutIn: TimerValue) : Ongoing() {
            override val type = when (reason) {
                is PreparingReason.WaitingForDeposit -> ReferendumStatusType.WAITING_DEPOSIT
                is PreparingReason.DecidingIn -> ReferendumStatusType.PREPARING
            }
        }

        data class InQueue(val timeOutIn: TimerValue, val position: TrackQueue.Position) : Ongoing() {
            override val type = ReferendumStatusType.IN_QUEUE
        }

        data class Deciding(val rejectIn: TimerValue) : Ongoing() {
            override val type = ReferendumStatusType.DECIDING
        }

        data class Confirming(val approveIn: TimerValue) : Ongoing() {
            override val type = ReferendumStatusType.CONFIRMING
        }
    }

    data class Approved(val since: BlockNumber, val executeIn: TimerValue) : ReferendumStatus() {
        override val type = ReferendumStatusType.APPROVED
    }

    object Executed : ReferendumStatus() {
        override val type = ReferendumStatusType.EXECUTED
    }

    sealed class NotExecuted : ReferendumStatus() {

        object TimedOut : NotExecuted() {
            override val type = ReferendumStatusType.TIMED_OUT
        }

        object Killed : NotExecuted() {
            override val type = ReferendumStatusType.KILLED
        }

        object Cancelled : NotExecuted() {
            override val type = ReferendumStatusType.CANCELLED
        }

        object Rejected : NotExecuted() {
            override val type = ReferendumStatusType.REJECTED
        }
    }
}

sealed class PreparingReason {

    object WaitingForDeposit : PreparingReason()

    data class DecidingIn(val timeLeft: TimerValue) : PreparingReason()
}

fun String.asReferendumStatusType(): ReferendumStatusType? = ReferendumStatusType.values().find { it.id == this }
