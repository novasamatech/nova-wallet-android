package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes.validations

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

class RemoveVotesValidationPayload(
    val fee: Fee,
    val asset: Asset,
)
