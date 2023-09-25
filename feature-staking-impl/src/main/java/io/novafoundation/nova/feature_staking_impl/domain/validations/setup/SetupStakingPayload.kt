package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class SetupStakingPayload(
    val maxFee: BigDecimal,
    val controllerAsset: Asset,
)
