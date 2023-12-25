package io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class RedeemValidationPayload(
    val fee: DecimalFee,
    val asset: Asset
)
