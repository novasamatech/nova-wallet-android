package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.commissionAssetToSpendOnBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.totalDeductedPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

data class SwapValidationPayload(
    val detailedAssetIn: SwapAssetData,
    val detailedAssetOut: SwapAssetData,
    val slippage: Percent,
    val feeAsset: Asset,
    val swapFee: SwapFee,
    val swapQuote: SwapQuote,
    val swapQuoteArgs: SwapQuoteArgs,
    val swapExecuteArgs: SwapExecuteArgs
) {

    data class SwapAssetData(
        val chain: Chain,
        val asset: Asset,
        val amountInPlanks: BigInteger
    )
}

val SwapValidationPayload.isFeePayingByAssetIn: Boolean
    get() = feeAsset.token.configuration.fullId == detailedAssetIn.asset.token.configuration.fullId

val SwapValidationPayload.swapAmountInFeeToken: BigInteger
    get() = if (isFeePayingByAssetIn) {
        detailedAssetIn.amountInPlanks
    } else {
        BigInteger.ZERO
    }

val SwapValidationPayload.toBuyAmountToKeepMainEDInFeeAsset: BigInteger
    get() = if (isFeePayingByAssetIn) {
        swapFee.minimumBalanceBuyIn.commissionAssetToSpendOnBuyIn
    } else {
        BigInteger.ZERO
    }

val SwapValidationPayload.totalDeductedAmountInFeeToken: BigInteger
    get() = if (isFeePayingByAssetIn) {
        swapFee.totalDeductedPlanks
    } else {
        BigInteger.ZERO
    }

val SwapValidationPayload.maxAmountToSwap: BigInteger
    get() = if (isFeePayingByAssetIn) {
        val maxAmount = detailedAssetIn.asset.transferableInPlanks - totalDeductedAmountInFeeToken
        maxAmount.coerceAtLeast(BigInteger.ZERO)
    } else {
        detailedAssetIn.asset.transferableInPlanks
    }
