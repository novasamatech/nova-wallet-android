package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model

import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionStylingSize
import java.math.RoundingMode

private const val INCLUDE_ZERO_FIAT = true
private const val INCLUDE_ASSET_TICKER: Boolean = true
private const val USE_ABBREVIATION: Boolean = true
private const val ESTIMATED_FIAT: Boolean = false
private val TOKEN_AMOUNT_SIGN: AmountSign = AmountSign.NONE
private val ROUNDING_MODE: RoundingMode = RoundingMode.FLOOR
private val TOKEN_FRACTION_STYLING_SIZE: FractionStylingSize = FractionStylingSize.Default

class FiatConfig(
    val roundingMode: RoundingMode = ROUNDING_MODE,
    val style: Style = Style.DEFAULT,
    val estimatedFiat: Boolean = ESTIMATED_FIAT,
    val fractionStylingSize: FractionStylingSize = TOKEN_FRACTION_STYLING_SIZE
) {
    enum class Style {
        DEFAULT, NO_ABBREVIATION, SIMPLE
    }
}

class TokenConfig(
    val includeAssetTicker: Boolean = INCLUDE_ASSET_TICKER,
    val useAbbreviation: Boolean = USE_ABBREVIATION,
    val tokenAmountSign: AmountSign = TOKEN_AMOUNT_SIGN,
    val roundingMode: RoundingMode = ROUNDING_MODE,
    val fractionStylingSize: FractionStylingSize = TOKEN_FRACTION_STYLING_SIZE
)

class AmountConfig(
    val includeZeroFiat: Boolean = INCLUDE_ZERO_FIAT,
    includeAssetTicker: Boolean = INCLUDE_ASSET_TICKER,
    useAbbreviation: Boolean = USE_ABBREVIATION,
    tokenAmountSign: AmountSign = TOKEN_AMOUNT_SIGN,
    roundingMode: RoundingMode = ROUNDING_MODE,
    estimatedFiat: Boolean = ESTIMATED_FIAT,
    tokenFractionStylingSize: FractionStylingSize = TOKEN_FRACTION_STYLING_SIZE,
    fiatFractionStylingSize: FractionStylingSize = TOKEN_FRACTION_STYLING_SIZE,
) {
    val tokenConfig: TokenConfig = TokenConfig(
        includeAssetTicker = includeAssetTicker,
        useAbbreviation = useAbbreviation,
        tokenAmountSign = tokenAmountSign,
        roundingMode = roundingMode,
        fractionStylingSize = tokenFractionStylingSize
    )

    val fiatConfig: FiatConfig = FiatConfig(
        roundingMode = roundingMode,
        style = if (useAbbreviation) FiatConfig.Style.DEFAULT else FiatConfig.Style.NO_ABBREVIATION,
        estimatedFiat = estimatedFiat,
        fractionStylingSize = fiatFractionStylingSize,
    )
}
