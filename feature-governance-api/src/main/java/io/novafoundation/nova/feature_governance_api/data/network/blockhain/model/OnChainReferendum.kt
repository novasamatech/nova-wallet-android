package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import java.math.BigInteger

@JvmInline
value class TrackId(val value: BigInteger) {

    override fun toString(): String {
        return value.toString()
    }
}

@JvmInline
value class ReferendumId(val value: BigInteger) {

    override fun toString(): String {
        return value.toString()
    }
}

class OnChainReferendum(
    val status: OnChainReferendumStatus,
    val id: ReferendumId,
)

sealed class OnChainReferendumStatus {

    class Ongoing(
        val track: TrackId,
        val proposal: Proposal,
        val submitted: BlockNumber,
        val submissionDeposit: ReferendumDeposit?,
        val decisionDeposit: ReferendumDeposit?,
        val deciding: DecidingStatus?,
        val tally: Tally,
        val inQueue: Boolean,
        val threshold: VotingThreshold,
        val delayedPassing: DelayedThresholdPassing
    ) : OnChainReferendumStatus()

    class Approved(override val since: BlockNumber) : OnChainReferendumStatus(), TimeSinceStatus

    class Rejected(override val since: BlockNumber) : OnChainReferendumStatus(), TimeSinceStatus

    class Cancelled(override val since: BlockNumber) : OnChainReferendumStatus(), TimeSinceStatus

    class TimedOut(override val since: BlockNumber) : OnChainReferendumStatus(), TimeSinceStatus

    class Killed(override val since: BlockNumber) : OnChainReferendumStatus(), TimeSinceStatus

    interface TimeSinceStatus {
        val since: BlockNumber
    }
}

sealed class Proposal {

    class Legacy(val hash: ByteArray) : Proposal()

    class Inline(val encodedCall: ByteArray, val call: GenericCall.Instance) : Proposal()

    class Lookup(val hash: ByteArray, val callLength: BigInteger) : Proposal()
}

class DecidingStatus(
    val since: BlockNumber,
    val confirming: ConfirmingSource
)

sealed class ConfirmingSource {

    class FromThreshold(val end: BlockNumber) : ConfirmingSource()

    class OnChain(val status: ConfirmingStatus?) : ConfirmingSource()
}

class ConfirmingStatus(val till: BlockNumber)

class Tally(
    // post-conviction
    val ayes: Balance,
    // post-conviction
    val nays: Balance,
    // pre-conviction
    val support: Balance
)

class ReferendumDeposit(
    val who: AccountId,
    val amount: Balance
)
