package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee

class ParachainStakingRedeemValidationPayload(
    val asset: Asset,
    val fee: DecimalFee,
)
