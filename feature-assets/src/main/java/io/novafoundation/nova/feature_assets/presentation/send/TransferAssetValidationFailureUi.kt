package io.novafoundation.nova.feature_assets.presentation.send

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount

fun mapAssetTransferValidationFailureToUI(
    resourceManager: ResourceManager,
    failure: AssetTransferValidationFailure
): TitleAndMessage {
    return when (failure) {
        is AssetTransferValidationFailure.DeadRecipient.InCommissionAsset -> {
            resourceManager.getString(R.string.wallet_send_dead_recipient_commission_asset_title) to
                resourceManager.getString(R.string.wallet_send_dead_recipient_commission_asset_message, failure.commissionAsset.symbol)
        }
        AssetTransferValidationFailure.DeadRecipient.InUsedAsset -> {
            resourceManager.getString(R.string.common_amount_low) to
                resourceManager.getString(R.string.wallet_send_dead_recipient_message)
        }
        AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.choose_amount_error_too_big)
        }
        is AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset -> {
            handleNotEnoughFeeError(failure, resourceManager)
        }

        AssetTransferValidationFailure.WillRemoveAccount.WillBurnDust -> {
            resourceManager.getString(R.string.wallet_send_existential_warning_title) to
                resourceManager.getString(R.string.wallet_send_existential_warning_message_v2_2_0)
        }
        is AssetTransferValidationFailure.WillRemoveAccount.WillTransferDust -> {
            resourceManager.getString(R.string.wallet_send_existential_warning_title) to
                resourceManager.getString(R.string.wallet_send_existential_warnining_transfer_dust)
        }
        is AssetTransferValidationFailure.InvalidRecipientAddress -> {
            resourceManager.getString(R.string.common_validation_invalid_address_title) to
                resourceManager.getString(R.string.common_validation_invalid_address_message, failure.chain.name)
        }
        is AssetTransferValidationFailure.PhishingRecipient -> {
            resourceManager.getString(R.string.wallet_send_phishing_warning_title) to
                resourceManager.getString(R.string.wallet_send_phishing_warning_text, failure.address)
        }
        AssetTransferValidationFailure.NonPositiveAmount -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.common_zero_amount_error)
        }
        is AssetTransferValidationFailure.NotEnoughFunds.ToPayCrossChainFee -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(
                    R.string.wallet_send_cannot_pay_cross_chain_fee,
                    failure.fee.formatTokenAmount(failure.usedAsset),
                    failure.remainingBalanceAfterTransfer.formatTokenAmount(failure.usedAsset)
                )
        }
        is AssetTransferValidationFailure.NotEnoughFunds.ToStayAboveED -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.wallet_send_insufficient_balance_commission, failure.commissionAsset.symbol)
        }
        AssetTransferValidationFailure.RecipientCannotAcceptTransfer -> {
            resourceManager.getString(R.string.wallet_send_recipient_blocked_title) to
                resourceManager.getString(R.string.wallet_send_recipient_blocked_message)
        }
    }
}
