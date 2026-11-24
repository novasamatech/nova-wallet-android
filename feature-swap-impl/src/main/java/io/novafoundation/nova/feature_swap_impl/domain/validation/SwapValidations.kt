package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.model.amountByExecutingAccount
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.domain.SwapInputReserve
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds
import io.novafoundation.nova.feature_swap_impl.domain.swapMaxFeeMargin
import io.novafoundation.nova.feature_swap_impl.domain.swapMaximum
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapCanPayExtraFeesValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapDoNotLooseAssetInDustValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapEnoughLiquidityValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapPoolTradeLimitsValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapPriceImpactValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapRateChangesValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapSlippageRangeValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.sufficientAmountOutToStayAboveEDValidation
import io.novafoundation.nova.feature_swap_impl.domain.withSwapMaxFeeMargin
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.decimalAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalanceConsideringConsumersValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalanceGeneric

typealias SwapValidationSystem = ValidationSystem<SwapValidationPayload, SwapValidationFailure>
typealias SwapValidation = Validation<SwapValidationPayload, SwapValidationFailure>
typealias SwapValidationSystemBuilder = ValidationSystemBuilder<SwapValidationPayload, SwapValidationFailure>

fun SwapValidationSystemBuilder.priceImpactValidation(
    priceImpactThresholds: PriceImpactThresholds
) = validate(SwapPriceImpactValidation(priceImpactThresholds))

fun SwapValidationSystemBuilder.availableSlippage(
    swapService: SwapService
) = validate(SwapSlippageRangeValidation(swapService))

fun SwapValidationSystemBuilder.swapFeeSufficientBalance(
    assetsValidationContext: AssetsValidationContext
) = validate(SwapCanPayExtraFeesValidation(assetsValidationContext))

fun SwapValidationSystemBuilder.swapSmallRemainingBalance(
    assetsValidationContext: AssetsValidationContext
) = validate(
    SwapDoNotLooseAssetInDustValidation(assetsValidationContext)
)

internal fun SwapValidationSystemBuilder.sufficientBalanceConsideringConsumersValidation(
    assetsValidationContext: AssetsValidationContext,
    swapInputReserve: SwapInputReserve
) = sufficientBalanceConsideringConsumersValidation(
    assetsValidationContext = assetsValidationContext,
    assetExtractor = { it.amountIn.chainAsset },
    canDropBelowEd = { payload, asset -> swapInputReserve.canDropBelowEd(asset, payload.fee.initialSubmissionFee.submissionOrigin.executingAccount) },
    feeExtractor = { it.fee.totalFeeAmount(it.amountIn.chainAsset) },
    amountExtractor = { it.amountIn.amount },
    error = { payload, existentialDeposit, asset ->
        val feeWithMargin = payload.fee.totalFeeAmount(payload.amountIn.chainAsset).withSwapMaxFeeMargin()
        SwapValidationFailure.InsufficientBalance.BalanceNotConsiderConsumers(
            assetIn = payload.amountIn.chainAsset,
            assetInED = existentialDeposit,
            maxSwapAmount = asset.swapMaximum(existentialDeposit, feeWithMargin),
            fee = payload.fee.initialSubmissionFee.amountByExecutingAccount,
            feeAsset = payload.fee.initialSubmissionFee.asset
        )
    }
)

fun SwapValidationSystemBuilder.rateNotExceedSlippage(
    sharedQuoteValidationRetriever: SharedQuoteValidationRetriever
) = validate(
    SwapRateChangesValidation(sharedQuoteValidationRetriever)
)

fun SwapValidationSystemBuilder.enoughLiquidity(
    sharedQuoteValidationRetriever: SharedQuoteValidationRetriever
) = validate(
    SwapEnoughLiquidityValidation(sharedQuoteValidationRetriever)
)

fun SwapValidationSystemBuilder.poolTradeLimits(
    assetsValidationContext: AssetsValidationContext
) = validate(
    SwapPoolTradeLimitsValidation(assetsValidationContext)
)

fun SwapValidationSystemBuilder.canPayAllFees(
    assetsValidationContext: AssetsValidationContext
) = validate(
    SwapCanPayExtraFeesValidation(assetsValidationContext)
)

fun SwapValidationSystemBuilder.enoughAssetInToPayForSwap(
    assetsValidationContext: AssetsValidationContext
) = sufficientBalanceGeneric(
    available = { assetsValidationContext.getAsset(it.amountIn.chainAsset).transferable },
    amount = { it.amountIn.decimalAmount },
    error = { SwapValidationFailure.NotEnoughFunds.InUsedAsset }
)

fun SwapValidationSystemBuilder.enoughAssetInToPayForSwapAndFee(
    assetsValidationContext: AssetsValidationContext
) = sufficientBalanceGeneric(
    available = {
        val asset = assetsValidationContext.getAsset(it.amountIn.chainAsset)
        // The generic validation subtracts and displays the fee separately.
        val available = asset.swapMaximum(it.fee.additionalMaxAmountDeduction.fromCountedTowardsEd)
        it.amountIn.chainAsset.amountFromPlanks(available)
    },
    amount = { it.amountIn.decimalAmount },
    fee = {
        val planks = it.fee.totalFeeAmount(it.amountIn.chainAsset)
        it.amountIn.chainAsset.amountFromPlanks(planks)
    },
    error = {
        val assetIn = it.payload.amountIn.chainAsset
        val feeMargin = it.payload.fee.totalFeeAmount(assetIn).swapMaxFeeMargin()

        SwapValidationFailure.InsufficientBalance.CannotPayFeeDueToAmount(
            assetIn = assetIn,
            feeAmount = it.fee,
            maxSwapAmount = (it.maxUsable - assetIn.amountFromPlanks(feeMargin)).atLeastZero()
        )
    }
)

fun SwapValidationSystemBuilder.sufficientAmountOutToStayAboveED(
    assetsValidationContext: AssetsValidationContext
) = sufficientAmountOutToStayAboveEDValidation(assetsValidationContext)

fun SwapValidationSystemBuilder.positiveAmountIn() = positiveAmount(
    amount = { it.amountIn.decimalAmount },
    error = { SwapValidationFailure.NonPositiveAmount }
)

fun SwapValidationSystemBuilder.positiveAmountOut() = positiveAmount(
    amount = { it.amountOut.decimalAmount },
    error = { SwapValidationFailure.NonPositiveAmount }
)
