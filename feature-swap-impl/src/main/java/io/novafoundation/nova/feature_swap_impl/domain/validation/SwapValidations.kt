package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapEnoughLiquidityValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapFeeSufficientBalanceValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapRateChangesValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapSlippageRangeValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapSmallRemainingBalanceValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.checkForFeeChanges
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import java.math.BigDecimal

typealias SwapValidationSystem = ValidationSystem<SwapValidationPayload, SwapValidationFailure>
typealias SwapValidation = Validation<SwapValidationPayload, SwapValidationFailure>
typealias SwapValidationSystemBuilder = ValidationSystemBuilder<SwapValidationPayload, SwapValidationFailure>

fun SwapValidationSystemBuilder.availableSlippage(assetExchange: AssetExchange) = validate(
    SwapSlippageRangeValidation(
        assetExchange = assetExchange
    )
)

fun SwapValidationSystemBuilder.swapFeeSufficientBalance() = validate(
    SwapFeeSufficientBalanceValidation()
)

fun SwapValidationSystemBuilder.swapSmallRemainingBalance(
    assetSourceRegistry: AssetSourceRegistry,
    chainRegistry: ChainRegistry
) = validate(
    SwapSmallRemainingBalanceValidation(
        assetSourceRegistry,
        chainRegistry
    )
)

fun SwapValidationSystemBuilder.rateNotExceedSlippage(sharedQuoteValidationRetriever: SharedQuoteValidationRetriever) = validate(
    SwapRateChangesValidation { sharedQuoteValidationRetriever.retrieveQuote(it).getOrThrow().swapRate() }
)

fun SwapValidationSystemBuilder.enoughLiquidity(sharedQuoteValidationRetriever: SharedQuoteValidationRetriever) = validate(
    SwapEnoughLiquidityValidation { sharedQuoteValidationRetriever.retrieveQuote(it) }
)

fun SwapValidationSystemBuilder.sufficientBalanceInFeeAsset() = sufficientBalance(
    available = { it.feeAsset.transferable },
    amount = { BigDecimal.ZERO },
    fee = { it.feeAsset.token.amountFromPlanks(it.swapFee.networkFee.amount) },
    error = { payload, availableToPayFees ->
        SwapValidationFailure.NotEnoughFunds.InCommissionAsset(
            chainAsset = payload.feeAsset.token.configuration,
            fee = payload.feeAsset.token.amountFromPlanks(payload.swapFee.networkFee.amount),
            availableToPayFees = availableToPayFees
        )
    }
)

fun SwapValidationSystemBuilder.sufficientBalanceInUsedAsset() = sufficientBalance(
    available = { it.detailedAssetIn.asset.transferable },
    amount = { it.detailedAssetIn.amount },
    fee = { BigDecimal.ZERO },
    error = { _, _ ->
        SwapValidationFailure.NotEnoughFunds.InUsedAsset
    }
)

fun SwapValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { BigDecimal.ZERO },
        total = { it.detailedAssetOut.asset.total + it.detailedAssetOut.amount },
        chainWithAsset = { ChainWithAsset(it.detailedAssetOut.chain, it.detailedAssetOut.asset.token.configuration) },
        error = { payload, existentialDeposit ->
            SwapValidationFailure.AmountOutIsTooLowToStayAboveED(
                payload.detailedAssetOut.asset.token.configuration,
                payload.detailedAssetOut.amount,
                existentialDeposit
            )
        }
    )
}

fun SwapValidationSystemBuilder.checkForFeeChanges(
    swapService: SwapService
) = checkForFeeChanges(
    calculateFee = {
        val swapFee = swapService.estimateFee(it.swapExecuteArgs)
        swapFee.networkFee
    },
    currentFee = { it.feeAsset.token.amountFromPlanks(it.swapFee.networkFee.amount) },
    chainAsset = { it.feeAsset.token.configuration },
    error = SwapValidationFailure::FeeChangeDetected
)
