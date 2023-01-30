package io.novafoundation.nova.feature_governance_api.domain.referendum.voters

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigDecimal

interface GenericVoter<V : GenericVoter.Vote?> {

    val vote: V

    val identity: Identity?

    val accountId: AccountId

    interface Vote {

        val totalVotes: BigDecimal
    }

    class ConvictionVote(val amount: BigDecimal, val conviction: Conviction) : Vote {
        override val totalVotes = amount * conviction.amountMultiplier()
    }
}
