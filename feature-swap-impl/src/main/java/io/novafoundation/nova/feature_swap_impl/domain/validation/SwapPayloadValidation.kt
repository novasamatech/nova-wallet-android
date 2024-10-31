package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

data class SwapValidationPayload(
    val detailedAssetIn: SwapAssetData,
    val detailedAssetOut: SwapAssetData,
    val slippage: Fraction,
    val feeAsset: Asset,
    val fee: SwapFee,
    val swapQuote: SwapQuote,
    val swapQuoteArgs: SwapQuoteArgs,
    val swapExecuteArgs: SwapFeeArgs
) {

    data class SwapAssetData(
        val chain: Chain,
        val asset: Asset,
        val amountInPlanks: Balance
    )
}

val SwapValidationPayload.isFeePayingByAssetIn: Boolean
    get() = feeAsset.token.configuration.fullId == detailedAssetIn.asset.token.configuration.fullId

val SwapValidationPayload.swapAmountInFeeToken: Balance
    get() = if (isFeePayingByAssetIn) {
        detailedAssetIn.amountInPlanks
    } else {
        BigInteger.ZERO
    }

val SwapValidationPayload.totalDeductedAmountInFeeToken: Balance
    get() = TODO()

val SwapValidationPayload.maxAmountToSwap: Balance
    get() = detailedAssetIn.asset.transferableInPlanks - fee.maxAmountDeductionForAssetIn
