package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class SetupStakingPayload(
    val maxFee: DecimalFee,
    val controllerAsset: Asset,
)
