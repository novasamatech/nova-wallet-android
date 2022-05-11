package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

class StartParachainStakingValidationPayload(
    val amount: BigDecimal,
    val fee: BigDecimal,
    val collator: Collator,
    val asset: Asset,
)
