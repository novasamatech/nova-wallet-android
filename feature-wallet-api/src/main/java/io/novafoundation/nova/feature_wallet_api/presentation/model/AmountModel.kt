package io.novafoundation.nova.feature_wallet_api.presentation.model

import androidx.annotation.DimenRes
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatWithFullAmount
import io.novafoundation.nova.common.utils.withTokenSymbol
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class AmountModel(
    val token: CharSequence,
    val fiat: CharSequence?
) {

    // Override it since SpannableString is not equals by content
    override fun equals(other: Any?): Boolean {
        return other is AmountModel &&
            other.token.toString() == token.toString() &&
            other.fiat?.toString() == fiat?.toString()
    }
}

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
    token: TokenBase,
    includeZeroFiat: Boolean = true,
    estimatedFiat: Boolean = false,
    tokenAmountSign: AmountSign = AmountSign.NONE,
): AmountModel = mapAmountToAmountModel(
    amount = token.amountFromPlanks(amountInPlanks),
    token = token,
    includeZeroFiat = includeZeroFiat,
    estimatedFiat = estimatedFiat,
    tokenAmountSign = tokenAmountSign
)

fun mapAmountToAmountModel(
    amount: BigDecimal,
    token: TokenBase,
    includeZeroFiat: Boolean = true,
    includeAssetTicker: Boolean = true,
    useAbbreviation: Boolean = true,
    tokenAmountSign: AmountSign = AmountSign.NONE,
    roundingMode: RoundingMode = RoundingMode.FLOOR,
    estimatedFiat: Boolean = false
): AmountModel {
    val fiatAmount = token.amountToFiat(amount)

    val unsignedTokenAmount = if (useAbbreviation) {
        if (includeAssetTicker) {
            amount.formatTokenAmount(token.configuration, roundingMode)
        } else {
            amount.format(roundingMode)
        }
    } else {
        val unformattedAmount = amount.formatWithFullAmount()

        if (includeAssetTicker) {
            unformattedAmount.withTokenSymbol(token.configuration.symbol)
        } else {
            unformattedAmount
        }
    }

    var formattedFiat = fiatAmount.takeIf { it != BigDecimal.ZERO || includeZeroFiat }
        ?.formatAsCurrency(token.currency, roundingMode)

    if (estimatedFiat && formattedFiat != null) {
        formattedFiat = "~$formattedFiat"
    }

    return AmountModel(
        token = tokenAmountSign.signSymbol + unsignedTokenAmount,
        fiat = formattedFiat
    )
}

fun mapAmountToAmountModel(
    amount: BigDecimal,
    asset: Asset,
    includeZeroFiat: Boolean = true,
    includeAssetTicker: Boolean = true,
    useAbbreviation: Boolean = true,
    tokenAmountSign: AmountSign = AmountSign.NONE,
    roundingMode: RoundingMode = RoundingMode.FLOOR
): AmountModel = mapAmountToAmountModel(
    amount = amount,
    token = asset.token,
    includeZeroFiat = includeZeroFiat,
    includeAssetTicker = includeAssetTicker,
    useAbbreviation = useAbbreviation,
    tokenAmountSign = tokenAmountSign,
    roundingMode = roundingMode
)

fun Asset.transferableFormat() = transferable.formatTokenAmount(token.configuration, RoundingMode.FLOOR)

fun Asset.transferableAmountModel() = mapAmountToAmountModel(transferable, this)

fun transferableAmountModelOf(asset: Asset) = mapAmountToAmountModel(asset.transferable, asset)

fun AmountModel.formatBalanceWithFraction(amountFormatter: AmountFormatter, @DimenRes floatAmountSize: Int): AmountModel {
    return AmountModel(
        token = token.formatBalanceWithFraction(amountFormatter, floatAmountSize),
        fiat = fiat
    )
}
