package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class RemoveVotesValidationPayload(
    val fee: BigDecimal,
    val asset: Asset,
)
