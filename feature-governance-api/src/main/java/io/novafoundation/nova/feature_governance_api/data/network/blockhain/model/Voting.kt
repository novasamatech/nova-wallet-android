package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import java.math.BigDecimal
import java.math.BigInteger

sealed class Voting {

    data class Casting(
        val votes: Map<ReferendumId, AccountVote>,
        val prior: PriorLock
    ) : Voting()

    // do not yet care about delegations
    object Delegating : Voting()
}

sealed class AccountVote {

    data class Standard(
        val vote: Vote,
        val balance: Balance
    ) : AccountVote()

    // do not yet care split votes
    object Split : AccountVote()
}

data class PriorLock(
    val unlockAt: BlockNumber,
    val amount: Balance,
)

data class VotesAmount(
    val total: BigDecimal,
    val amount: BigDecimal,
    val multiplier: BigDecimal,
)

enum class VoteType {
    AYE, NAY
}

fun Voting.trackVotesNumber(): Int {
    return when(this) {
        is Voting.Casting -> votes.size
        Voting.Delegating -> 0
    }
}

fun AccountVote.votes(chainAsset: Chain.Asset): VotesAmount? {
    return when (this) {
        // TODO handle split votes
        AccountVote.Split -> null
        is AccountVote.Standard -> {
            val amount = chainAsset.amountFromPlanks(balance)
            val total = vote.conviction.votesFor(amount)

            VotesAmount(
                total = total,
                amount = amount,
                multiplier = vote.conviction.amountMultiplier()
            )
        }
    }
}

fun AccountVote.isAye(): Boolean? {
    return voteType()?.let { it == VoteType.AYE }
}

fun AccountVote.voteType(): VoteType? {
    return when (this) {
        AccountVote.Split -> null

        is AccountVote.Standard -> if (vote.aye) {
            VoteType.AYE
        } else {
            VoteType.NAY
        }
    }
}

fun Voting.votes(): Map<ReferendumId, AccountVote> {
    return when (this) {
        is Voting.Casting -> votes
        Voting.Delegating -> emptyMap()
    }
}

fun AccountVote.completedReferendumLockDuration(referendumOutcome: VoteType, lockPeriod: BlockNumber): BlockNumber {
    return when (this) {
        AccountVote.Split -> BlockNumber.ZERO

        is AccountVote.Standard -> {
            val approved = referendumOutcome == VoteType.AYE

            // vote has the same direction as outcome
            if (approved == vote.aye) {
                vote.conviction.lockDuration(lockPeriod)
            } else {
                BlockNumber.ZERO
            }
        }
    }
}

fun AccountVote.maxLockDuration(lockPeriod: BlockNumber): BlockNumber {
    return when (this) {
        AccountVote.Split -> BigInteger.ZERO
        is AccountVote.Standard -> vote.conviction.lockDuration(lockPeriod)
    }
}

fun Conviction.lockDuration(lockPeriod: BlockNumber): BlockNumber {
    return lockPeriods() * lockPeriod
}

fun Conviction.lockPeriods(): BigInteger {
    val multiplier = when (this) {
        Conviction.None -> 0
        Conviction.Locked1x -> 1
        Conviction.Locked2x -> 2
        Conviction.Locked3x -> 4
        Conviction.Locked4x -> 8
        Conviction.Locked5x -> 16
        Conviction.Locked6x -> 32
    }

    return multiplier.toBigInteger()
}

fun Conviction.votesFor(amount: BigDecimal): BigDecimal {
    return amountMultiplier() * amount
}

fun Conviction.amountMultiplier(): BigDecimal {
    val multiplier: Double = when (this) {
        Conviction.None -> 0.1
        Conviction.Locked1x -> 1.0
        Conviction.Locked2x -> 2.0
        Conviction.Locked3x -> 3.0
        Conviction.Locked4x -> 4.0
        Conviction.Locked5x -> 5.0
        Conviction.Locked6x -> 6.0
    }

    return multiplier.toBigDecimal()
}
