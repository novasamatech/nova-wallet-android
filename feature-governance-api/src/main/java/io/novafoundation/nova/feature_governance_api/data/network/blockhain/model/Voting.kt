package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote

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
