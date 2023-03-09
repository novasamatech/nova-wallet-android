package io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class RebagValidationPayload(
    val fee: BigDecimal,
    val asset: Asset,
)
