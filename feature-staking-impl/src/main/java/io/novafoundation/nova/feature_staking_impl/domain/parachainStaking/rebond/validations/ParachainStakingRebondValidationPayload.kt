package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class ParachainStakingRebondValidationPayload(
    val asset: Asset,
    val fee: DecimalFee,
)
