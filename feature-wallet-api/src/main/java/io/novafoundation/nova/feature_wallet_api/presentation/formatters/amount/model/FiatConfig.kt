package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model

import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionStylingSize

class FiatConfig(
    val tokenFractionStylingSize: FractionStylingSize = FractionStylingSize.Default
)
