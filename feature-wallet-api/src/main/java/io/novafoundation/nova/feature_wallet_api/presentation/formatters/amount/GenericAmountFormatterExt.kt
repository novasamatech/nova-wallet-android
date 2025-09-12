package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun <T> GenericAmountFormatter<T>.formatAmountToAmountModel(
    amountInPlanks: BigInteger,
    asset: Asset,
    config: AmountConfig = AmountConfig(
        includeAssetTicker = true,
        roundingMode = RoundingMode.FLOOR
    )
): T = formatAmountToAmountModel(
    amount = asset.token.amountFromPlanks(amountInPlanks),
    token = asset.token,
    config = config
)

fun <T> GenericAmountFormatter<T>.formatAmountToAmountModel(
    amountInPlanks: BigInteger,
    token: TokenBase,
    config: AmountConfig = AmountConfig()
): T = formatAmountToAmountModel(
    amount = token.amountFromPlanks(amountInPlanks),
    token = token,
    config = config
)

fun <T> GenericAmountFormatter<T>.formatAmountToAmountModel(
    amount: BigDecimal,
    asset: Asset,
    config: AmountConfig = AmountConfig()
): T = formatAmountToAmountModel(
    amount = amount,
    token = asset.token,
    config = config
)

fun <T> Asset.transferableAmountModel(amountFormatter: GenericAmountFormatter<T>) = amountFormatter.formatAmountToAmountModel(transferable, this)

fun <T> transferableAmountModelOf(amountFormatter: GenericAmountFormatter<T>, asset: Asset) =
    amountFormatter.formatAmountToAmountModel(asset.transferable, asset)
