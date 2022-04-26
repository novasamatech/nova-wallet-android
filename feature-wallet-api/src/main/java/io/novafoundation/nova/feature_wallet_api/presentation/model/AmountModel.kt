package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import java.math.BigDecimal
import java.math.BigInteger

data class AmountModel(
    val token: String,
    val fiat: String?
)

enum class AmountSign(val signSymbol: String) {
    NONE(""), NEGATIVE("-"), POSITIVE("+")
}

fun mapAmountToAmountModel(
    amountInPlanks: BigInteger,
    asset: Asset
): AmountModel = mapAmountToAmountModel(
    amount = asset.token.amountFromPlanks(amountInPlanks),
    asset = asset
)

fun mapAmountToAmountModel(
    amountInPlanks: BigInteger,
    token: Token
): AmountModel = mapAmountToAmountModel(
    amount = token.amountFromPlanks(amountInPlanks),
    token = token
)

fun mapAmountToAmountModel(
    amount: BigDecimal,
    token: Token,
    includeZeroFiat: Boolean = true,
    tokenAmountSign: AmountSign = AmountSign.NONE,
): AmountModel {
    val fiatAmount = token.fiatAmount(amount)

    return AmountModel(
        token = tokenAmountSign.signSymbol + amount.formatTokenAmount(token.configuration),
        fiat = fiatAmount.takeIf { it != BigDecimal.ZERO || includeZeroFiat }?.formatAsCurrency()
    )
}

fun mapAmountToAmountModel(
    amount: BigDecimal,
    asset: Asset,
    includeZeroFiat: Boolean = true,
    tokenAmountSign: AmountSign = AmountSign.NONE
): AmountModel = mapAmountToAmountModel(amount, asset.token, includeZeroFiat, tokenAmountSign)

fun transferableAmountModelOf(asset: Asset) = mapAmountToAmountModel(asset.transferable, asset)
