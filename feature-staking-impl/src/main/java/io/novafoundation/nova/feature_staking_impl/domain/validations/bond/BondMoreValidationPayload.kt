package io.novafoundation.nova.feature_staking_impl.domain.validations.bond

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class BondMoreValidationPayload(
    val stashAddress: String,
    val fee: BigDecimal,
    val amount: BigDecimal,
    val stashAsset: Asset,
)
