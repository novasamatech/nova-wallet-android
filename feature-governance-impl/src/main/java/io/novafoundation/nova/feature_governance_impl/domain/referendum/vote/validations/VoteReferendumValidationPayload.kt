package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import java.math.BigDecimal

data class VoteReferendumValidationPayload(
    val onChainReferendum: OnChainReferendum,
    val asset: Asset,
    val trackVoting: Voting?,
    val voteAmount: BigDecimal,
    val fee: DecimalFee,
    val voteType: VoteType,
    val conviction: Conviction
)
