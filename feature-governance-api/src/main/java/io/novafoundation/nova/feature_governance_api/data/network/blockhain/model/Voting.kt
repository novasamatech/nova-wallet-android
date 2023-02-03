package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ConvictionVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import java.math.BigDecimal
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId

sealed class Voting {

    data class Casting(
        val votes: Map<ReferendumId, AccountVote>,
        val prior: PriorLock
    ) : Voting()

    class Delegating(
        val amount: Balance,
        val target: AccountId,
        val conviction: Conviction,
        val prior: PriorLock
    ) : Voting()
}

sealed class AccountVote {

    data class Standard(
        val vote: Vote,
        val balance: Balance
    ) : AccountVote()

    // TODO split & split abstain votes
    object Unsupported : AccountVote()
}

data class PriorLock(
    val unlockAt: BlockNumber,
    val amount: Balance,
)

enum class VoteType {
    AYE, NAY
}

fun Voting.trackVotesNumber(): Int {
    return when (this) {
        is Voting.Casting -> votes.size
        is Voting.Delegating -> 0
    }
}

fun Voting.votedReferenda(): Collection<ReferendumId> {
    return when (this) {
        is Voting.Casting -> votes.keys
        is Voting.Delegating -> emptyList()
    }
}

fun AyeVote(amount: Balance, conviction: Conviction) = AccountVote.Standard(
    vote = Vote(
        aye = true,
        conviction = conviction
    ),
    balance = amount
)

fun AccountVote.votes(chainAsset: Chain.Asset): GenericVoter.ConvictionVote? {
    return ConvictionVote(this, chainAsset)
}

fun AccountVote.amount(): Balance {
    return when (this) {
        AccountVote.Unsupported -> Balance.ZERO // TODO not yet supported
        is AccountVote.Standard -> balance
    }
}

fun AccountVote.isAye(): Boolean? {
    return voteType()?.let { it == VoteType.AYE }
}

fun AccountVote.voteType(): VoteType? {
    return when (this) {
        AccountVote.Unsupported -> null

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
        is Voting.Delegating -> emptyMap()
    }
}

fun Voting.totalLock(): Balance {
    return when (this) {
        is Voting.Casting -> {
            val fromVotes = votes.maxOfOrNull { it.value.amount() }.orZero()

            fromVotes.max(prior.amount)
        }

        is Voting.Delegating -> amount.max(prior.amount)
    }
}

fun AccountVote.completedReferendumLockDuration(referendumOutcome: VoteType, lockPeriod: BlockNumber): BlockNumber {
    return when (this) {
        AccountVote.Unsupported -> BlockNumber.ZERO

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
        AccountVote.Unsupported -> BigInteger.ZERO
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

fun Voting.Delegating.getConvictionVote(chainAsset: Chain.Asset): GenericVoter.ConvictionVote {
    return GenericVoter.ConvictionVote(chainAsset.amountFromPlanks(amount), conviction)
}
