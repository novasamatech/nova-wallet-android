package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigDecimal

class VoteReferendumValidationPayload(
    val onChainReferendum: OnChainReferendum,
    val asset: Asset,
    val trackVoting: Voting?,
    val voteAmount: BigDecimal,
    val fee: DecimalFee
)
