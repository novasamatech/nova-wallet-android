package io.novafoundation.nova.feature_staking_impl.domain.validations.rebond

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class RebondValidationPayload(
    val controllerAsset: Asset,
    val fee: BigDecimal,
    val rebondAmount: BigDecimal
)
