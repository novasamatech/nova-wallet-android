package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.commissionAssetToSpendOnBuyIn
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

data class SwapValidationPayload(
    val detailedAssetIn: SwapAssetData,
    val outDetails: SwapAssetData,
    val slippage: Percent,
    val feeAsset: Asset,
    val swapFee: SwapFee,
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

val SwapValidationPayload.feeAmountInFeeToken: BigInteger
    get() = if (isFeePayingByAssetIn) {
        swapFee.networkFee.amount
    } else {
        BigInteger.ZERO
    }

val SwapValidationPayload.toBuyEDInFeeAsset: BigInteger
    get() = if (feeAsset.token.configuration.fullId == detailedAssetIn.asset.token.configuration.fullId) {
        swapFee.minimumBalanceBuyIn.commissionAssetToSpendOnBuyIn
    } else {
        BigInteger.ZERO
    }

val SwapValidationPayload.totalDeductedAmountInFeeToken: BigInteger
    get() {
        val feeAmountInFeeToken = feeAmountInFeeToken
        val toBuyExistentialDeposit = toBuyEDInFeeAsset

        return feeAmountInFeeToken + toBuyExistentialDeposit
    }

val SwapValidationPayload.maxAmountToSwap: BigInteger
    get() = if (feeAsset.token.configuration.fullId == detailedAssetIn.asset.token.configuration.fullId) {
        detailedAssetIn.asset.transferableInPlanks - totalDeductedAmountInFeeToken
    } else {
        detailedAssetIn.asset.transferableInPlanks
    }

fun SwapValidationSystemBuilder.positiveAmount() = positiveAmount(
    amount = { it.detailedAssetIn.amount },
    error = { SwapValidationFailure.NonPositiveAmount }
)
