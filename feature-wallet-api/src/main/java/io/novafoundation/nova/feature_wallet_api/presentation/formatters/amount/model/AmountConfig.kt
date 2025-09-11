package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model

import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionStylingSize
import java.math.RoundingMode

class AmountConfig(
    val includeZeroFiat: Boolean = true,
    val includeAssetTicker: Boolean = true,
    val useAbbreviation: Boolean = true,
    val tokenAmountSign: AmountSign = AmountSign.NONE,
    val roundingMode: RoundingMode = RoundingMode.FLOOR,
    val estimatedFiat: Boolean = false,
    val tokenFractionStylingSize: FractionStylingSize = FractionStylingSize.Default
)
