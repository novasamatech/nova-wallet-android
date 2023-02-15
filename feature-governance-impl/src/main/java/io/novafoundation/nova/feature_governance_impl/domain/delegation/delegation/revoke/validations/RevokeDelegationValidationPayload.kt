package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class RevokeDelegationValidationPayload(
    val fee: BigDecimal,
    val asset: Asset,
)
