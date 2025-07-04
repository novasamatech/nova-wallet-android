package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.AmountOutIsTooLowToStayAboveED
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InsufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InvalidSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NewRateExceededSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NonPositiveAmount
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NotEnoughFunds
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NotEnoughLiquidity
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.TooSmallRemainingBalance
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleNonPositiveAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

fun mapSwapValidationFailureToUI(
    resourceManager: ResourceManager,
    status: ValidationStatus.NotValid<SwapValidationFailure>,
    actions: ValidationFlowActions<*>,
    amountInSwapMaxAction: () -> Unit,
    amountOutSwapMinAction: (Chain.Asset, Balance) -> Unit
): TransformedFailure {
    return when (val reason = status.reason) {
        is NotEnoughFunds.ToPayFeeAndStayAboveED -> handleInsufficientBalanceCommission(reason, resourceManager).asDefault()

        NotEnoughFunds.InUsedAsset -> resourceManager.amountIsTooBig().asDefault()

        is InvalidSlippage -> TitleAndMessage(
            resourceManager.getString(R.string.swap_invalid_slippage_failure_title),
            resourceManager.getString(R.string.swap_invalid_slippage_failure_message, reason.minSlippage.formatPercents(), reason.maxSlippage.formatPercents())
        ).asDefault()

        is SwapValidationFailure.HighPriceImpact -> highPriceImpact(reason, resourceManager, actions)

        is NewRateExceededSlippage -> TitleAndMessage(
            resourceManager.getString(R.string.swap_rate_was_updated_failure_title),
            resourceManager.getString(
                R.string.swap_rate_was_updated_failure_message,
                BigDecimal.ONE.formatTokenAmount(reason.assetIn),
                reason.selectedRate.formatTokenAmount(reason.assetOut),
                reason.newRate.formatTokenAmount(reason.assetOut)
            )
        ).asDefault()

        NonPositiveAmount -> handleNonPositiveAmount(resourceManager).asDefault()

        NotEnoughLiquidity -> TitleAndMessage(resourceManager.getString(R.string.swap_not_enought_liquidity_failure), second = null).asDefault()

        is AmountOutIsTooLowToStayAboveED -> handleErrorToSwapMin(reason, resourceManager, amountOutSwapMinAction)

        is InsufficientBalance.CannotPayFeeDueToAmount -> handleInsufficientBalance(
            title = resourceManager.getString(R.string.common_not_enough_funds_title),
            message = resourceManager.getString(
                R.string.swap_failure_insufficient_balance_message,
                reason.maxSwapAmount.formatTokenAmount(reason.assetIn),
                reason.feeAmount.formatTokenAmount(reason.assetIn)
            ),
            resourceManager = resourceManager,
            positiveButtonClick = amountInSwapMaxAction
        )

        is InsufficientBalance.BalanceNotConsiderConsumers -> TitleAndMessage(
            resourceManager.getString(R.string.common_not_enough_funds_title),
            resourceManager.getString(
                R.string.swap_failure_balance_not_consider_consumers,
                reason.assetInED.formatPlanks(reason.assetIn),
                reason.fee.formatPlanks(reason.feeAsset)
            )
        ).asDefault()

        is InsufficientBalance.BalanceNotConsiderInsufficientReceiveAsset -> TitleAndMessage(
            resourceManager.getString(R.string.common_not_enough_funds_title),
            resourceManager.getString(
                R.string.swap_failure_balance_not_consider_non_sufficient_assets,
                reason.existentialDeposit.formatPlanks(reason.assetIn),
                reason.assetOut.symbol
            )
        ).asDefault()

        is TooSmallRemainingBalance -> handleTooSmallRemainingBalance(
            title = resourceManager.getString(R.string.swap_failure_too_small_remaining_balance_title),
            message = resourceManager.getString(
                R.string.swap_failure_too_small_remaining_balance_message,
                reason.assetInExistentialDeposit.formatPlanks(reason.assetIn),
                reason.remainingBalance.formatPlanks(reason.assetIn)
            ),
            resourceManager = resourceManager,
            actions = actions,
            positiveButtonClick = amountInSwapMaxAction
        )

        is InsufficientBalance.CannotPayFee -> TitleAndMessage(
            resourceManager.getString(R.string.common_not_enough_funds_title),
            resourceManager.getString(
                R.string.common_not_enough_to_pay_fee_message,
                reason.fee.formatPlanks(reason.feeAsset),
                reason.balance.formatPlanks(reason.feeAsset)
            )
        ).asDefault()

        is SwapValidationFailure.IntermediateAmountOutIsTooLowToStayAboveED -> TitleAndMessage(
            resourceManager.getString(R.string.swap_too_low_amount_to_stay_abow_ed_title),
            resourceManager.getString(
                R.string.swap_intermediate_too_low_amount_to_stay_abow_ed_message,
                reason.amount.formatPlanks(reason.asset),
                reason.existentialDeposit.formatPlanks(reason.asset)
            )
        ).asDefault()

        is SwapValidationFailure.CannotReceiveAssetOut -> TitleAndMessage(
            resourceManager.getString(R.string.common_not_enough_funds_title),
            resourceManager.getString(
                R.string.swap_failure_cannot_receive_insufficient_asset_out,
                reason.requiredNativeAssetOnChainOut.formatPlanks(),
                reason.destination.chain.name,
                reason.destination.asset.symbol
            )
        ).asDefault()
    }
}

fun handleInsufficientBalance(
    title: String,
    message: String,
    resourceManager: ResourceManager,
    positiveButtonClick: () -> Unit
): TransformedFailure {
    return handleErrorToSwapMax(
        title = title,
        message = message,
        resourceManager = resourceManager,
        negativeButtonText = resourceManager.getString(R.string.common_cancel),
        positiveButtonClick = positiveButtonClick,
        negativeButtonClick = { }
    )
}

fun handleTooSmallRemainingBalance(
    title: String,
    message: String,
    resourceManager: ResourceManager,
    actions: ValidationFlowActions<*>,
    positiveButtonClick: () -> Unit
): TransformedFailure {
    return handleErrorToSwapMax(
        title = title,
        message = message,
        resourceManager = resourceManager,
        negativeButtonText = resourceManager.getString(R.string.common_proceed),
        positiveButtonClick = positiveButtonClick,
        negativeButtonClick = { actions.resumeFlow() }
    )
}

fun handleErrorToSwapMax(
    title: String,
    message: String,
    resourceManager: ResourceManager,
    negativeButtonText: String,
    positiveButtonClick: () -> Unit,
    negativeButtonClick: () -> Unit
): TransformedFailure {
    return TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = title,
            message = message,
            customStyle = R.style.AccentAlertDialogTheme,
            okAction = CustomDialogDisplayer.Payload.DialogAction(
                title = resourceManager.getString(R.string.swap_failure_swap_max_button),
                action = positiveButtonClick
            ),
            cancelAction = CustomDialogDisplayer.Payload.DialogAction(
                title = negativeButtonText,
                action = negativeButtonClick
            )
        )
    )
}

fun handleErrorToSwapMin(
    reason: AmountOutIsTooLowToStayAboveED,
    resourceManager: ResourceManager,
    swapMinAmountAction: (Chain.Asset, BigInteger) -> Unit
): TransformedFailure {
    return TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = resourceManager.getString(R.string.swap_too_low_amount_to_stay_abow_ed_title),
            message = resourceManager.getString(
                R.string.swap_too_low_amount_to_stay_abow_ed_message,
                reason.amountInPlanks.formatPlanks(reason.asset),
                reason.existentialDeposit.formatPlanks(reason.asset),
            ),
            customStyle = R.style.AccentAlertDialogTheme,
            okAction = CustomDialogDisplayer.Payload.DialogAction(
                title = resourceManager.getString(R.string.swap_failure_swap_min_button),
                action = { swapMinAmountAction(reason.asset, reason.existentialDeposit) }
            ),
            cancelAction = CustomDialogDisplayer.Payload.DialogAction(
                title = resourceManager.getString(R.string.common_cancel),
                action = { }
            )
        )
    )
}

fun highPriceImpact(
    reason: SwapValidationFailure.HighPriceImpact,
    resourceManager: ResourceManager,
    actions: ValidationFlowActions<*>
): TransformedFailure {
    return TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = resourceManager.getString(R.string.high_price_impact_detacted_title, reason.priceImpact.formatPercents()),
            message = resourceManager.getString(R.string.high_price_impact_detacted_message),
            customStyle = R.style.AccentNegativeAlertDialogTheme_Reversed,
            okAction = CustomDialogDisplayer.Payload.DialogAction(
                title = resourceManager.getString(R.string.common_continue),
                action = {
                    actions.resumeFlow()
                }
            ),
            cancelAction = CustomDialogDisplayer.Payload.DialogAction(
                title = resourceManager.getString(R.string.common_cancel),
                action = { }
            )
        )
    )
}
