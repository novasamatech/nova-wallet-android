package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_account_api.data.model.amountByRequestedAccount
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.AmountOutIsTooLowToStayAboveED
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.FeeChangeDetected
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
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleFeeSpikeDetected
import io.novafoundation.nova.feature_wallet_api.domain.validation.notSufficientBalanceToPayFeeErrorMessage
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleNonPositiveAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal
import java.math.BigInteger

fun CoroutineScope.mapSwapValidationFailureToUI(
    resourceManager: ResourceManager,
    status: ValidationStatus.NotValid<SwapValidationFailure>,
    actions: ValidationFlowActions<*>,
    setNewFee: (SwapFee) -> Unit,
    amountInSwapMaxAction: () -> Unit,
    amountOutSwapMinAction: (Chain.Asset, Balance) -> Unit
): TransformedFailure? {
    return when (val reason = status.reason) {
        is NotEnoughFunds.ToPayFeeAndStayAboveED -> handleInsufficientBalanceCommission(reason, resourceManager).asDefault()

        NotEnoughFunds.InUsedAsset -> resourceManager.amountIsTooBig().asDefault()

        NotEnoughFunds.ToPayFee -> resourceManager.notSufficientBalanceToPayFeeErrorMessage().asDefault()

        is InvalidSlippage -> TitleAndMessage(
            resourceManager.getString(R.string.swap_invalid_slippage_failure_title),
            resourceManager.getString(R.string.swap_invalid_slippage_failure_message, reason.minSlippage.format(), reason.maxSlippage.format())
        ).asDefault()

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

        is TooSmallRemainingBalance.NoNeedsToBuyMainAssetED -> handleTooSmallRemainingBalance(
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

        is TooSmallRemainingBalance.NeedsToBuyMainAssetED -> handleTooSmallRemainingBalance(
            title = resourceManager.getString(R.string.swap_failure_too_small_remaining_balance_title),
            message = resourceManager.getString(
                R.string.swap_failure_too_small_remaining_balance_with_buy_ed_message,
                reason.assetInExistentialDeposit.formatPlanks(reason.assetIn),
                reason.fee.amountByRequestedAccount.formatPlanks(reason.feeAsset),
                reason.toSellAmountToKeepEDUsingAssetIn.formatPlanks(reason.assetIn),
                reason.toBuyAmountToKeepEDInCommissionAsset.formatPlanks(reason.nativeAsset),
                reason.nativeAsset.symbol,
                reason.remainingBalance.formatPlanks(reason.assetIn)
            ),
            resourceManager = resourceManager,
            actions = actions,
            positiveButtonClick = amountInSwapMaxAction
        )

        is InsufficientBalance.NoNeedsToBuyMainAssetED -> handleInsufficientBalance(
            title = resourceManager.getString(R.string.common_not_enough_funds_title),
            message = resourceManager.getString(
                R.string.swap_failure_insufficient_balance_message,
                reason.maxSwapAmount.formatPlanks(reason.assetIn),
                reason.fee.amountByRequestedAccount.formatPlanks(reason.feeAsset)
            ),
            resourceManager = resourceManager,
            positiveButtonClick = amountInSwapMaxAction
        )

        is InsufficientBalance.NeedsToBuyMainAssetED -> handleInsufficientBalance(
            title = resourceManager.getString(R.string.common_not_enough_funds_title),
            message = resourceManager.getString(
                R.string.swap_failure_insufficient_balance_with_buy_ed_message,
                reason.maxSwapAmount.formatPlanks(reason.assetIn),
                reason.fee.amountByRequestedAccount.formatPlanks(reason.feeAsset),
                reason.toSellAmountToKeepEDUsingAssetIn.formatPlanks(reason.assetIn),
                reason.toBuyAmountToKeepEDInCommissionAsset.formatPlanks(reason.nativeAsset),
                reason.nativeAsset.symbol
            ),
            resourceManager = resourceManager,
            positiveButtonClick = amountInSwapMaxAction
        )

        is InsufficientBalance.BalanceNotConsiderConsumers -> TitleAndMessage(
            resourceManager.getString(R.string.common_not_enough_funds_title),
            resourceManager.getString(
                R.string.swap_failure_balance_not_consider_consumers,
                reason.existentialDeposit.formatPlanks(reason.nativeAsset),
                reason.swapFee.networkFee.amountByRequestedAccount.formatPlanks(reason.feeAsset)
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

        is FeeChangeDetected -> handleFeeSpikeDetected(
            error = reason,
            resourceManager = resourceManager,
            actions = actions,
            setFee = { setNewFee(it.newFee.genericFee) },
        )
    }
}

fun CoroutineScope.handleInsufficientBalance(
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

fun CoroutineScope.handleTooSmallRemainingBalance(
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

fun CoroutineScope.handleErrorToSwapMax(
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

fun CoroutineScope.handleErrorToSwapMin(
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
