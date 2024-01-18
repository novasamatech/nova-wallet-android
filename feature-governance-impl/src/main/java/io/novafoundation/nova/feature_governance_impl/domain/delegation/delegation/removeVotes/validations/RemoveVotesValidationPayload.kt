package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class RemoveVotesValidationPayload(
    val fee: DecimalFee,
    val asset: Asset,
)
