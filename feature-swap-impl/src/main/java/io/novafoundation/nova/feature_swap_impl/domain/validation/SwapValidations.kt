package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapEnoughLiquidityValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapFeeSufficientBalanceValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapRateChangesValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapSlippageRangeValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapSmallRemainingBalanceValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.checkForFeeChanges
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalanceConsideringConsumersValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalanceGeneric
import java.math.BigDecimal

typealias SwapValidationSystem = ValidationSystem<SwapValidationPayload, SwapValidationFailure>
typealias SwapValidation = Validation<SwapValidationPayload, SwapValidationFailure>
typealias SwapValidationSystemBuilder = ValidationSystemBuilder<SwapValidationPayload, SwapValidationFailure>

fun SwapValidationSystemBuilder.availableSlippage(swapService: SwapService) = validate(
    SwapSlippageRangeValidation(swapService)
)

fun SwapValidationSystemBuilder.swapFeeSufficientBalance() = validate(
    SwapFeeSufficientBalanceValidation()
)

fun SwapValidationSystemBuilder.swapSmallRemainingBalance(
    assetSourceRegistry: AssetSourceRegistry
) = validate(
    SwapSmallRemainingBalanceValidation(
        assetSourceRegistry
    )
)

fun SwapValidationSystemBuilder.sufficientBalanceConsideringConsumersValidation(
    assetSourceRegistry: AssetSourceRegistry
) = sufficientBalanceConsideringConsumersValidation(
    assetSourceRegistry,
    chainExtractor = { it.detailedAssetIn.chain },
    assetExtractor = { it.detailedAssetIn.asset.token.configuration },
    balanceCountedTowardsEdExtractor = { it.detailedAssetIn.asset.balanceCountedTowardsEDInPlanks },
    feeExtractor = { it.totalDeductedAmountInFeeToken },
    amountExtractor = { it.detailedAssetIn.amountInPlanks },
    error = { payload, existentialDeposit ->
        SwapValidationFailure.InsufficientBalance.BalanceNotConsiderConsumers(
            nativeAsset = payload.detailedAssetIn.asset.token.configuration,
            feeAsset = payload.feeAsset.token.configuration,
            swapFee = payload.fee,
            existentialDeposit = existentialDeposit
        )
    }
)

fun SwapValidationSystemBuilder.rateNotExceedSlippage(sharedQuoteValidationRetriever: SharedQuoteValidationRetriever) = validate(
    SwapRateChangesValidation { sharedQuoteValidationRetriever.retrieveQuote(it).getOrThrow() }
)

fun SwapValidationSystemBuilder.enoughLiquidity(sharedQuoteValidationRetriever: SharedQuoteValidationRetriever) = validate(
    SwapEnoughLiquidityValidation { sharedQuoteValidationRetriever.retrieveQuote(it) }
)

fun SwapValidationSystemBuilder.sufficientBalanceInFeeAsset() = sufficientBalanceGeneric(
    available = { it.feeAsset.transferable },
    amount = { BigDecimal.ZERO },
    fee = { it.fee },
    error = { SwapValidationFailure.NotEnoughFunds.ToPayFee }
)

fun SwapValidationSystemBuilder.sufficientBalanceInUsedAsset() = sufficientBalance(
    available = { it.detailedAssetIn.asset.transferable },
    amount = { it.detailedAssetIn.asset.token.amountFromPlanks(it.detailedAssetIn.amountInPlanks) },
    fee = { null },
    error = { SwapValidationFailure.NotEnoughFunds.InUsedAsset }
)

fun SwapValidationSystemBuilder.sufficientAssetOutBalanceToStayAboveED(
    assetSourceRegistry: AssetSourceRegistry
) = sufficientAmountOutToStayAboveEDValidation(assetSourceRegistry)

fun SwapValidationSystemBuilder.checkForFeeChanges(
    swapService: SwapService
) = checkForFeeChanges(
    calculateFee = { swapService.estimateFee(it.swapExecuteArgs) },
    currentFee = { it.fee },
    chainAsset = { it.feeAsset.token.configuration },
    error = SwapValidationFailure::FeeChangeDetected
)

fun SwapValidationSystemBuilder.positiveAmountIn() = positiveAmount(
    amount = { it.detailedAssetIn.asset.token.amountFromPlanks(it.detailedAssetIn.amountInPlanks) },
    error = { SwapValidationFailure.NonPositiveAmount }
)

fun SwapValidationSystemBuilder.positiveAmountOut() = positiveAmount(
    amount = { it.detailedAssetOut.asset.token.amountFromPlanks(it.detailedAssetOut.amountInPlanks) },
    error = { SwapValidationFailure.NonPositiveAmount }
)
