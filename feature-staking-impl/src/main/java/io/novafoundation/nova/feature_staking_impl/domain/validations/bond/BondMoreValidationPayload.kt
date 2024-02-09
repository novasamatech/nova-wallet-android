package io.novafoundation.nova.feature_staking_impl.domain.validations.bond

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigDecimal

class BondMoreValidationPayload(
    val stashAddress: String,
    val fee: DecimalFee,
    val amount: BigDecimal,
    val stashAsset: Asset,
)
