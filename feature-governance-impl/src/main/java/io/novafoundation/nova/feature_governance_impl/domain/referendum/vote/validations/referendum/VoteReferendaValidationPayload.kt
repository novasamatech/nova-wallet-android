package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.VoteValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import java.math.BigDecimal

data class VoteReferendaValidationPayload(
    override val onChainReferenda: List<OnChainReferendum>,
    override val asset: Asset,
    override val trackVoting: List<Voting>,
    override val maxAmount: BigDecimal,
    val voteType: VoteType?,
    val conviction: Conviction?,
    override val fee: Fee
) : VoteValidationPayload
