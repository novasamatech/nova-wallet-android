package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class ParachainStakingRebondValidationPayload(
    val asset: Asset,
    val fee: BigDecimal,
)
