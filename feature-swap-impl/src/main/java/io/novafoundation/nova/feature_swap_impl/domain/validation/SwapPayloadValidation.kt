package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.commissionAssetToSpendOnBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.totalDeductedPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
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
        val amount: BigDecimal
    )
}

val SwapValidationPayload.isFeePayingByAssetIn: Boolean
    get() = feeAsset.token.configuration.fullId == detailedAssetIn.asset.token.configuration.fullId

val SwapValidationPayload.swapAmountInFeeToken: BigInteger
    get() = if (isFeePayingByAssetIn) {
        detailedAssetIn.asset.token.planksFromAmount(detailedAssetIn.amount)
    } else {
        BigInteger.ZERO
    }

val SwapValidationPayload.toBuyAmountToKeepEDInFeeAsset: BigInteger
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
        detailedAssetIn.asset.transferableInPlanks - totalDeductedAmountInFeeToken
    } else {
        detailedAssetIn.asset.transferableInPlanks
    }
