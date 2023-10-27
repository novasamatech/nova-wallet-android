package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.TransformedFailure.Default
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_account_api.domain.validation.handleSystemAccountValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleFeeSpikeDetected
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun CoroutineScope.mapAssetTransferValidationFailureToUI(
    resourceManager: ResourceManager,
    status: ValidationStatus.NotValid<AssetTransferValidationFailure>,
    actions: ValidationFlowActions<*>,
    feeLoaderMixin: FeeLoaderMixin.Presentation,
): TransformedFailure? {
    return when (val reason = status.reason) {
        is AssetTransferValidationFailure.DeadRecipient.InCommissionAsset -> Default(
            resourceManager.getString(R.string.wallet_send_dead_recipient_commission_asset_title) to
                resourceManager.getString(R.string.wallet_send_dead_recipient_commission_asset_message, reason.commissionAsset.symbol)
        )

        AssetTransferValidationFailure.DeadRecipient.InUsedAsset -> Default(
            resourceManager.getString(R.string.common_amount_low) to
                resourceManager.getString(R.string.wallet_send_dead_recipient_message)
        )

        AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset -> Default(
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.choose_amount_error_too_big)
        )

        is AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset -> Default(
            handleNotEnoughFeeError(reason, resourceManager)
        )

        AssetTransferValidationFailure.WillRemoveAccount.WillBurnDust -> Default(
            resourceManager.getString(R.string.wallet_send_existential_warning_title) to
                resourceManager.getString(R.string.wallet_send_existential_warning_message_v2_2_0)
        )

        is AssetTransferValidationFailure.WillRemoveAccount.WillTransferDust -> Default(
            resourceManager.getString(R.string.wallet_send_existential_warning_title) to
                resourceManager.getString(R.string.wallet_send_existential_warnining_transfer_dust)
        )

        is AssetTransferValidationFailure.InvalidRecipientAddress -> Default(
            resourceManager.getString(R.string.common_validation_invalid_address_title) to
                resourceManager.getString(R.string.common_validation_invalid_address_message, reason.chain.name)
        )

        is AssetTransferValidationFailure.PhishingRecipient -> Default(
            resourceManager.getString(R.string.wallet_send_phishing_warning_title) to
                resourceManager.getString(R.string.wallet_send_phishing_warning_text, reason.address)
        )

        AssetTransferValidationFailure.NonPositiveAmount -> Default(
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.common_zero_amount_error)
        )

        is AssetTransferValidationFailure.NotEnoughFunds.ToPayCrossChainFee -> Default(
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(
                    R.string.wallet_send_cannot_pay_cross_chain_fee,
                    reason.fee.formatTokenAmount(reason.usedAsset),
                    reason.remainingBalanceAfterTransfer.formatTokenAmount(reason.usedAsset)
                )
        )

        is AssetTransferValidationFailure.NotEnoughFunds.ToStayAboveED -> handleInsufficientBalanceCommission(
            reason,
            resourceManager
        ).asDefault()

        AssetTransferValidationFailure.RecipientCannotAcceptTransfer -> Default(
            resourceManager.getString(R.string.wallet_send_recipient_blocked_title) to
                resourceManager.getString(R.string.wallet_send_recipient_blocked_message)
        )

        is AssetTransferValidationFailure.FeeChangeDetected -> handleFeeSpikeDetected(
            error = reason,
            resourceManager = resourceManager,
            feeLoaderMixin = feeLoaderMixin,
            actions = actions,
        )

        AssetTransferValidationFailure.RecipientIsSystemAccount -> Default(
            handleSystemAccountValidationFailure(resourceManager)
        )
    }
}

fun CoroutineScope.mapFailure(
    resourceManager: ResourceManager,
    status: ValidationStatus.NotValid<SwapValidationFailure>,
    actions: ValidationFlowActions<*>,
    feeLoaderMixin: FeeLoaderMixin.Presentation,
): TransformedFailure? {
    return when (val reason = status.reason) {
        SwapValidationFailure.NotEnoughFunds -> Default(TitleAndMessage("NotEnoughFunds", ""))
        is SwapValidationFailure.InsufficientBalance.NoNeedsToBuyMainAssetED -> Default(TitleAndMessage("InsufficientBalance.NativeFee", ""))
        is SwapValidationFailure.InsufficientBalance.NeedsToBuyMainAssetED -> Default(
            TitleAndMessage(
                "InsufficientBalanceWithCustomFee.CustomFee", ""
            )
        )

        SwapValidationFailure.InvalidSlippage -> Default(TitleAndMessage("InvalidSlippage", ""))
        SwapValidationFailure.NewRateExceededSlippage -> Default(TitleAndMessage("NewRateExceededSlippage", ""))
        SwapValidationFailure.NonPositiveAmount -> Default(TitleAndMessage("NonPositiveAmount", ""))
        SwapValidationFailure.NotEnoughLiquidity -> Default(TitleAndMessage("NotEnoughLiquidity", ""))
        is SwapValidationFailure.ToStayAboveED -> Default(TitleAndMessage("ToStayAboveED", ""))
        is SwapValidationFailure.TooSmallAmount -> Default(TitleAndMessage("TooSmallAmount", ""))
        is SwapValidationFailure.TooSmallAmountWithCustomFee -> Default(TitleAndMessage("TooSmallAmountWithCustomFee", ""))
        is SwapValidationFailure.TooSmallRemainingBalance.NeedsToBuyMainAssetED -> Default(
            TitleAndMessage(
                "TooSmallRemainingBalance.CustomFee",
                ""
            )
        )

        is SwapValidationFailure.TooSmallRemainingBalance.NoNeedsToBuyMainAssetED -> Default(
            TitleAndMessage(
                "TooSmallRemainingBalance.NativeFee",
                ""
            )
        )

        is SwapValidationFailure.FeeChangeDetected -> handleFeeSpikeDetected(
            error = reason,
            resourceManager = resourceManager,
            feeLoaderMixin = feeLoaderMixin,
            actions = actions,
        )
    }
}

fun CoroutineScope.handleTooSmallRemainingBalance(
    title: String,
    message: String,
    resourceManager: ResourceManager,
    actions: ValidationFlowActions<*>
): TransformedFailure {
    return TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = title,
            message = message,
            customStyle = R.style.AccentNegativeAlertDialogTheme,
            okAction = CustomDialogDisplayer.Payload.DialogAction(
                title = resourceManager.getString(R.string.common_proceed),
                action = {
                    launch {
                        feeLoaderMixin.setFee(error.payload.newFee.fee)
                    }
                }
            ),
            cancelAction = CustomDialogDisplayer.Payload.DialogAction(
                title = resourceManager.getString(R.string.common_proceed),
                action = { actions.resumeFlow() }
            )
        )
    )
}
