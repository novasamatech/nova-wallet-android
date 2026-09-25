package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model

import io.novafoundation.nova.common.utils.images.Icon

data class BittensorValidatorUi(
    val name: String,
    val icon: Icon?
)

data class BittensorFeeUi(
    val amount: String,
    val fiatAmount: String?
)
