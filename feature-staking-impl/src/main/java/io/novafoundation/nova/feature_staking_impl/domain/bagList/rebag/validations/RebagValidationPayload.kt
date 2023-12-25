package io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class RebagValidationPayload(
    val fee: DecimalFee,
    val asset: Asset,
)
