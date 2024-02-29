package io.novafoundation.nova.feature_governance_api.domain.referendum.voters

import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novasama.substrate_sdk_android.runtime.AccountId
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

fun SplitVote(amount: BigDecimal): GenericVoter.ConvictionVote = GenericVoter.ConvictionVote(amount, Conviction.None)
fun SplitVote(planks: Balance, chainAsset: Chain.Asset): GenericVoter.ConvictionVote {
    return GenericVoter.ConvictionVote(chainAsset.amountFromPlanks(planks), Conviction.None)
}
