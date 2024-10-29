package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.tindergov

import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.VoteValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_account_api.data.model.Fee
import java.math.BigDecimal

data class VoteTinderGovValidationPayload(
    override val onChainReferenda: List<OnChainReferendum>,
    override val asset: Asset,
    override val trackVoting: List<Voting>,
    override val fee: Fee,
    val basket: List<TinderGovBasketItem>
) : VoteValidationPayload {

    override val maxAmount: BigDecimal
        get() {
            val amount = basket.maxOf { it.amount }
            return asset.token.amountFromPlanks(amount)
        }
}
