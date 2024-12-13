package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import java.math.BigDecimal

interface VoteValidationPayload {

    val onChainReferenda: List<OnChainReferendum>

    val asset: Asset

    val trackVoting: List<Voting>

    val maxAmount: BigDecimal

    val fee: Fee
}
