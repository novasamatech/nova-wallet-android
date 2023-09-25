package io.novafoundation.nova.feature_assets.presentation.send

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.TransformedFailure.Default
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_account_api.domain.validation.handleSystemAccountValidationFailure
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleFeeSpikeDetected
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission
import kotlinx.coroutines.CoroutineScope

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

fun autoFixSendValidationPayload(
    payload: AssetTransferPayload,
    failureReason: AssetTransferValidationFailure
) = when (failureReason) {
    is AssetTransferValidationFailure.WillRemoveAccount.WillTransferDust -> payload.copy(
        transfer = payload.transfer.copy(
            amount = payload.transfer.amount + failureReason.dust
        )
    )

    is AssetTransferValidationFailure.FeeChangeDetected -> payload.copy(
        transfer = payload.transfer.copy(
            decimalFee = failureReason.payload.newFee
        )
    )

    else -> payload
}
