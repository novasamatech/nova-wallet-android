package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class RevokeDelegationValidationPayload(
    val fee: DecimalFee,
    val asset: Asset,
)
