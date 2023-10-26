package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

typealias SwapValidationSystem = ValidationSystem<SwapValidationPayload, SwapValidationFailure>
typealias SwapValidation = Validation<SwapValidationPayload, SwapValidationFailure>
typealias SwapValidationSystemBuilder = ValidationSystemBuilder<SwapValidationPayload, SwapValidationFailure>

sealed class SwapValidationFailure {

    object NonPositiveAmount : SwapValidationFailure()

    object InvalidSlippage : SwapValidationFailure()

    object NewRateExceededSlippage : SwapValidationFailure()

    object NotEnoughLiquidity : SwapValidationFailure()

    object NotEnoughFunds : SwapValidationFailure()

    class ToStayAboveED(val asset: Chain.Asset) : SwapValidationFailure()
}

data class SwapValidationPayload(
    val inDetails: SwapAssetData,
    val outDetails: SwapAssetData,
    val slippage: Percent,
    val swapFee: SwapFee,
    val swapQuoteArgs: SwapQuoteArgs
) {
    data class SwapAssetData(
        val chain: Chain,
        val asset: Asset,
        val amount: BigDecimal
    )
}

fun SwapValidationSystemBuilder.positiveAmount() = positiveAmount(
    amount = { it.inDetails.amount },
    error = { SwapValidationFailure.NonPositiveAmount }
)

fun SwapValidationSystemBuilder.availableSlippage(assetExchange: AssetExchange) = validate(
    SwapSlippageRangeValidation(
        assetExchange = assetExchange
    )
)

fun SwapValidationSystemBuilder.rateNotExceedSlippage(sharedQuoteValidationRetriever: SharedQuoteValidationRetriever) = validate(
    SwapRateChangesValidation { sharedQuoteValidationRetriever.retrieveQuote(it).getOrThrow().swapRate() }
)

fun SwapValidationSystemBuilder.enoughLiquidity(sharedQuoteValidationRetriever: SharedQuoteValidationRetriever) = validate(
    SwapEnoughLiquidityValidation { sharedQuoteValidationRetriever.retrieveQuote(it) }
)

fun SwapValidationSystemBuilder.sufficientBalanceInUsedAsset() = sufficientBalance(
    available = { it.inDetails.asset.transferable },
    amount = { it.inDetails.amount },
    fee = { BigDecimal.ZERO },
    error = { _, _ -> SwapValidationFailure.NotEnoughFunds }
)

fun SwapValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { BigDecimal.ZERO },
        total = { it.outDetails.amount + it.outDetails.asset.total },
        chainWithAsset = { ChainWithAsset(it.outDetails.chain, it.outDetails.asset.token.configuration) },
        error = { SwapValidationFailure.ToStayAboveED(it.outDetails.chain.utilityAsset) }
    )
}
