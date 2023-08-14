package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class AmountModel(
    val token: String,
    val fiat: String?
)

enum class AmountSign(val signSymbol: String) {
    NONE(""), NEGATIVE("-"), POSITIVE("+")
}

fun mapAmountToAmountModel(
    amountInPlanks: BigInteger,
    asset: Asset,
    includeAssetTicker: Boolean = true,
    roundingMode: RoundingMode = RoundingMode.FLOOR
): AmountModel = mapAmountToAmountModel(
    amount = asset.token.amountFromPlanks(amountInPlanks),
    asset = asset,
    includeAssetTicker = includeAssetTicker,
    roundingMode = roundingMode
)

fun mapAmountToAmountModel(
    amountInPlanks: BigInteger,
    token: Token,
    includeZeroFiat: Boolean = true,
): AmountModel = mapAmountToAmountModel(
    amount = token.amountFromPlanks(amountInPlanks),
    token = token,
    includeZeroFiat = includeZeroFiat
)

fun mapAmountToAmountModel(
    amount: BigDecimal,
    token: Token,
    includeZeroFiat: Boolean = true,
    includeAssetTicker: Boolean = true,
    tokenAmountSign: AmountSign = AmountSign.NONE,
    roundingMode: RoundingMode = RoundingMode.FLOOR
): AmountModel {
    val fiatAmount = token.amountToFiat(amount)

    val unsignedTokenAmount = if (includeAssetTicker) {
        amount.formatTokenAmount(token.configuration, roundingMode)
    } else {
        amount.format(roundingMode)
    }

    return AmountModel(
        token = tokenAmountSign.signSymbol + unsignedTokenAmount,
        fiat = fiatAmount.takeIf { it != BigDecimal.ZERO || includeZeroFiat }?.formatAsCurrency(token.currency, roundingMode)
    )
}

fun mapAmountToAmountModel(
    amount: BigDecimal,
    asset: Asset,
    includeZeroFiat: Boolean = true,
    includeAssetTicker: Boolean = true,
    tokenAmountSign: AmountSign = AmountSign.NONE,
    roundingMode: RoundingMode = RoundingMode.FLOOR
): AmountModel = mapAmountToAmountModel(
    amount = amount,
    token = asset.token,
    includeZeroFiat = includeZeroFiat,
    includeAssetTicker = includeAssetTicker,
    tokenAmountSign = tokenAmountSign,
    roundingMode = roundingMode
)

fun Asset.transferableAmountModel() = mapAmountToAmountModel(transferable, this)

fun transferableAmountModelOf(asset: Asset) = mapAmountToAmountModel(asset.transferable, asset)
