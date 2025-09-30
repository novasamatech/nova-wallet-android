package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model

import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig.AbbreviationStyle
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionPartStyling
import java.math.RoundingMode

private const val INCLUDE_ZERO_FIAT = true
private const val INCLUDE_ASSET_TICKER: Boolean = true
private const val USE_TOKEN_ABBREVIATION: Boolean = true
private const val ESTIMATED_FIAT: Boolean = false
private val FIAT_ABBREVIATION: AbbreviationStyle = AbbreviationStyle.DEFAULT_ABBREVIATION
private val TOKEN_AMOUNT_SIGN: AmountSign = AmountSign.NONE
private val ROUNDING_MODE: RoundingMode = RoundingMode.FLOOR
private val TOKEN_FRACTION_STYLING_SIZE: FractionPartStyling = FractionPartStyling.NoStyle

class FiatConfig(
    val roundingMode: RoundingMode = ROUNDING_MODE,
    val abbreviationStyle: AbbreviationStyle = FIAT_ABBREVIATION,
    val estimatedFiat: Boolean = ESTIMATED_FIAT,
    val fractionPartStyling: FractionPartStyling = TOKEN_FRACTION_STYLING_SIZE
) {
    enum class AbbreviationStyle {
        DEFAULT_ABBREVIATION, NO_ABBREVIATION, SIMPLE_ABBREVIATION
    }
}

class TokenConfig(
    val includeAssetTicker: Boolean = INCLUDE_ASSET_TICKER,
    val useAbbreviation: Boolean = USE_TOKEN_ABBREVIATION,
    val tokenAmountSign: AmountSign = TOKEN_AMOUNT_SIGN,
    val roundingMode: RoundingMode = ROUNDING_MODE,
    val fractionPartStyling: FractionPartStyling = TOKEN_FRACTION_STYLING_SIZE
)

class AmountConfig(
    val includeZeroFiat: Boolean = INCLUDE_ZERO_FIAT,
    val tokenConfig: TokenConfig = TokenConfig(),
    val fiatConfig: FiatConfig = FiatConfig()
) {

    constructor(
        includeZeroFiat: Boolean = INCLUDE_ZERO_FIAT,
        includeAssetTicker: Boolean = INCLUDE_ASSET_TICKER,
        useTokenAbbreviation: Boolean = USE_TOKEN_ABBREVIATION,
        fiatAbbreviation: AbbreviationStyle = FIAT_ABBREVIATION,
        tokenAmountSign: AmountSign = TOKEN_AMOUNT_SIGN,
        roundingMode: RoundingMode = ROUNDING_MODE,
        estimatedFiat: Boolean = ESTIMATED_FIAT,
        tokenFractionPartStyling: FractionPartStyling = TOKEN_FRACTION_STYLING_SIZE,
        fiatFractionPartStyling: FractionPartStyling = TOKEN_FRACTION_STYLING_SIZE,
    ) : this(
        includeZeroFiat,
        TokenConfig(
            includeAssetTicker = includeAssetTicker,
            useAbbreviation = useTokenAbbreviation,
            tokenAmountSign = tokenAmountSign,
            roundingMode = roundingMode,
            fractionPartStyling = tokenFractionPartStyling
        ),
        FiatConfig(
            roundingMode = roundingMode,
            abbreviationStyle = fiatAbbreviation,
            estimatedFiat = estimatedFiat,
            fractionPartStyling = fiatFractionPartStyling,
        ),
    )
}
