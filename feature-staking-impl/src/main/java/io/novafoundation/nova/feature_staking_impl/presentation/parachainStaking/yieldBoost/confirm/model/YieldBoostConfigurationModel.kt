package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model

import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class YieldBoostConfigurationModel(
    val mode: String,
    val frequency: String?,
    val threshold: AmountModel?,
    val termsText: String?,
)
