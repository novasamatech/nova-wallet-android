package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import androidx.annotation.DimenRes
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun AmountFormatter.formatAmountToAmountModel(
    amountInPlanks: BigInteger,
    asset: Asset,
    config: AmountConfig = AmountConfig(
        includeAssetTicker = true,
        roundingMode = RoundingMode.FLOOR
    )
): AmountModel = formatAmountToAmountModel(
    amount = asset.token.amountFromPlanks(amountInPlanks),
    token = asset.token,
    config = config
)

fun AmountFormatter.formatAmountToAmountModel(
    amountInPlanks: BigInteger,
    token: TokenBase,
    config: AmountConfig = AmountConfig()
): AmountModel = formatAmountToAmountModel(
    amount = token.amountFromPlanks(amountInPlanks),
    token = token,
    config = config
)

fun AmountFormatter.formatAmountToAmountModel(
    amount: BigDecimal,
    asset: Asset,
    config: AmountConfig = AmountConfig()
): AmountModel = formatAmountToAmountModel(
    amount = amount,
    token = asset.token,
    config = config
)

fun Asset.transferableAmountModel(amountFormatter: AmountFormatter) = amountFormatter.formatAmountToAmountModel(transferable, this)

fun transferableAmountModelOf(amountFormatter: AmountFormatter, asset: Asset) = amountFormatter.formatAmountToAmountModel(asset.transferable, asset)
