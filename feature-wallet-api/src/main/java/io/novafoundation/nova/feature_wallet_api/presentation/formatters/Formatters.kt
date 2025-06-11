package io.novafoundation.nova.feature_wallet_api.presentation.formatters

import io.novafoundation.nova.common.utils.SemiUnboundedRange
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.withTokenSymbol
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun BigInteger.formatPlanks(chainAsset: Chain.Asset, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return chainAsset.amountFromPlanks(this).formatTokenAmount(chainAsset, roundingMode)
}

fun ChainAssetWithAmount.formatPlanks(roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return amount.formatPlanks(chainAsset, roundingMode)
}

fun SemiUnboundedRange<Balance>.formatPlanksRange(chainAsset: Chain.Asset): String {
    val end = endInclusive
    val startFormatted = chainAsset.amountFromPlanks(start).format()

    return if (end != null) {
        val endFormatted = end.formatPlanks(chainAsset)

        "$startFormatted — $endFormatted"
    } else {
        "$startFormatted+".withTokenSymbol(chainAsset.symbol)
    }
}

fun BigDecimal.formatTokenAmount(
    chainAsset: Chain.Asset,
    roundingMode: RoundingMode = RoundingMode.FLOOR
): String {
    return formatTokenAmount(chainAsset.symbol, roundingMode)
}

fun BigDecimal.formatTokenAmount(
    tokenSymbol: String,
    roundingMode: RoundingMode = RoundingMode.FLOOR
): String {
    return formatTokenAmount(
        tokenSymbol.asTokenSymbol(),
        roundingMode
    )
}

fun BigDecimal.formatTokenChange(chainAsset: Chain.Asset, isIncome: Boolean): String {
    val withoutSign = formatTokenAmount(chainAsset)
    val sign = if (isIncome) '+' else '-'

    return sign + withoutSign
}
