package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchTime
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

@JvmInline
value class TrackId(val value: BigInteger)

@JvmInline
value class ReferendumId(val value: BigInteger)

class OnChainReferendum(
    val status: OnChainReferendumStatus,
    val id: ReferendumId,
)

sealed class OnChainReferendumStatus {

    class Ongoing(
        val track: TrackId,
        val proposalHash: ByteArray,
        val desiredEnactment: DispatchTime,
        val submitted: BlockNumber,
        val decisionDeposit: DecisionDeposit?,
        val deciding: DecidingStatus?,
        val tally: Tally,
        val inQueue: Boolean,
    ) : OnChainReferendumStatus()

    object Approved : OnChainReferendumStatus()

    object Rejected : OnChainReferendumStatus()

    object Cancelled : OnChainReferendumStatus()

    object TimedOut : OnChainReferendumStatus()

    object Killed : OnChainReferendumStatus()
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

class DecisionDeposit(
    val who: AccountId,
    val amount: Balance
)
