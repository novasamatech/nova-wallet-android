package io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class UnlockReferendumValidationPayload(
    val fee: BigDecimal,
    val asset: Asset,
)
