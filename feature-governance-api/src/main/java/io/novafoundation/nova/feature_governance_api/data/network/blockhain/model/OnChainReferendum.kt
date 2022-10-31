package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
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
        val submissionDeposit: ReferendumDeposit,
        val decisionDeposit: ReferendumDeposit?,
        val deciding: DecidingStatus?,
        val tally: Tally,
        val inQueue: Boolean,
        val threshold: VotingThreshold,
    ) : OnChainReferendumStatus()

    class Approved(val since: BlockNumber) : OnChainReferendumStatus()

    class Rejected(val since: BlockNumber) : OnChainReferendumStatus()

    class Cancelled(val since: BlockNumber) : OnChainReferendumStatus()

    class TimedOut(val since: BlockNumber) : OnChainReferendumStatus()

    class Killed(val since: BlockNumber) : OnChainReferendumStatus()
}

sealed class Proposal {

    class Legacy(val hash: ByteArray) : Proposal()

    class Inline(val encodedCall: ByteArray, val call: GenericCall.Instance) : Proposal()

    class Lookup(val hash: ByteArray, val callLength: BigInteger) : Proposal()
}

class DecidingStatus(
    val since: BlockNumber,
    val confirming: ConfirmingStatus?
)

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
