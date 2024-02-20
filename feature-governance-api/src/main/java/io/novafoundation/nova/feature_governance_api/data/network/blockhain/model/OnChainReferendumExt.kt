package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.common.utils.filterValuesIsInstance
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting.Approval
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.AccountId

fun OnChainReferendum.proposal(): Proposal? {
    return status.asOngoingOrNull()?.proposal
}

fun Proposal.hash(): ByteArray {
    return when (this) {
        is Proposal.Inline -> encodedCall.blake2b256()
        is Proposal.Legacy -> hash
        is Proposal.Lookup -> hash
    }
}

fun OnChainReferendum.track(): TrackId? {
    return status.asOngoingOrNull()?.track
}

fun OnChainReferendum.submissionDeposit(): ReferendumDeposit? {
    return when (status) {
        is OnChainReferendumStatus.Ongoing -> status.submissionDeposit
        else -> null
    }
}

fun OnChainReferendumStatus.inQueue(): Boolean {
    return this is OnChainReferendumStatus.Ongoing && inQueue
}

fun OnChainReferendumStatus.asOngoing(): OnChainReferendumStatus.Ongoing {
    return asOngoingOrNull() ?: error("Referendum is not ongoing")
}

fun OnChainReferendumStatus.sinceOrThrow(): BlockNumber {
    return asTimeSinceStatusOrNull()?.since ?: error("Status doesn't have since field")
}

fun OnChainReferendumStatus.asOngoingOrNull(): OnChainReferendumStatus.Ongoing? {
    return castOrNull<OnChainReferendumStatus.Ongoing>()
}

fun OnChainReferendumStatus.asTimeSinceStatusOrNull(): OnChainReferendumStatus.TimeSinceStatus? {
    return castOrNull<OnChainReferendumStatus.TimeSinceStatus>()
}

fun Tally.ayeVotes(): Approval.Votes {
    return votesOf(Tally::ayes)
}

fun Tally.nayVotes(): Approval.Votes {
    return votesOf(Tally::nays)
}

fun Map<TrackId, Voting>.flattenCastingVotes(): Map<ReferendumId, AccountVote> {
    return flatMap { (_, voting) ->
        when (voting) {
            is Voting.Casting -> voting.votes.toList()
            is Voting.Delegating -> emptyList()
        }
    }.toMap()
}

fun Map<TrackId, Voting>.delegations(to: AccountId? = null): Map<TrackId, Voting.Delegating> {
    val onlyDelegations = filterValuesIsInstance<TrackId, Voting.Delegating>()

    return if (to != null) {
        onlyDelegations.filterValues { it.target.contentEquals(to) }
    } else {
        onlyDelegations
    }
}

val OnChainReferendumStatus.Ongoing.proposer: AccountId?
    get() = submissionDeposit?.who

fun OnChainReferendumStatus.Ongoing.proposerDeposit(): Balance? {
    return proposer?.let(::depositBy)
}

fun OnChainReferendumStatus.Ongoing.depositBy(accountId: AccountId): Balance {
    return submissionDeposit.amountBy(accountId) + decisionDeposit.amountBy(accountId)
}

fun ReferendumDeposit?.amountBy(accountId: AccountId): Balance {
    if (this == null) return Balance.ZERO

    return amount.takeIf { who.contentEquals(accountId) }.orZero()
}

@Suppress("FunctionName")
private fun EmptyVotes() = Approval.Votes(
    amount = Balance.ZERO,
    fraction = Perbill.ZERO
)

private inline fun Tally.votesOf(field: (Tally) -> Balance): Approval.Votes {
    val totalVotes = ayes + nays

    if (totalVotes == Balance.ZERO) return EmptyVotes()

    val amount = field(this)
    val fraction = amount.divideToDecimal(totalVotes)

    return Approval.Votes(
        amount = amount,
        fraction = fraction
    )
}
