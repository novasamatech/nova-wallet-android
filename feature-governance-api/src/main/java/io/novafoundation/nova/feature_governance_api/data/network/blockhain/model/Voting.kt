package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import java.math.BigDecimal

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

fun AccountVote.votes(chainAsset: Chain.Asset): BigDecimal? {
    return when (this) {
        // TODO handle split votes
        AccountVote.Split -> null
        is AccountVote.Standard -> {
            val amountMultiplier = vote.conviction.amountMultiplier()
            val amount = chainAsset.amountFromPlanks(balance)

            amountMultiplier * amount
        }
    }
}

fun AccountVote.isAye(): Boolean? {
    return when (this) {
        // TODO handle split votes
        AccountVote.Split -> null
        is AccountVote.Standard -> vote.aye
    }
}

private fun Conviction.amountMultiplier(): BigDecimal {
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
