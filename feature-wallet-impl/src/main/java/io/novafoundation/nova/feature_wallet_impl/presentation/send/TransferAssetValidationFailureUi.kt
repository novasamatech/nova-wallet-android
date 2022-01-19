package io.novafoundation.nova.feature_wallet_impl.presentation.send

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_impl.R

fun mapAssetTransferValidationFailureToUI(
    resourceManager: ResourceManager,
    failure: AssetTransferValidationFailure
): TitleAndMessage {
    return when(failure) {
        is AssetTransferValidationFailure.DeadRecipient.InCommissionAsset -> {
            resourceManager.getString(R.string.wallet_send_dead_recipient_commission_asset_title) to
                resourceManager.getString(R.string.wallet_send_dead_recipient_commission_asset_message, failure.commissionAsset.symbol)
        }
        AssetTransferValidationFailure.DeadRecipient.InUsedAsset -> {
            resourceManager.getString(R.string.common_amount_low) to
                resourceManager.getString(R.string.wallet_send_dead_recipient_message)
        }
        AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset -> {
            resourceManager.getString(R.string.wallet_send_insufficient_balance_title) to
                resourceManager.getString(R.string.choose_amount_error_too_big)
        }
        is AssetTransferValidationFailure.NotEnoughFunds.InCommissionAsset -> {
            resourceManager.getString(R.string.wallet_send_insufficient_balance_title) to
                resourceManager.getString(R.string.wallet_send_insufficient_balance_commission, failure.commissionAsset.symbol)
        }
        AssetTransferValidationFailure.WillRemoveAccount.WillBurnDust -> {
            resourceManager.getString(R.string.wallet_send_existential_warning_title) to
                resourceManager.getString(R.string.wallet_send_existential_warning_message_v2_2_0)
        }
        is AssetTransferValidationFailure.WillRemoveAccount.WillTransferDust -> {
            resourceManager.getString(R.string.wallet_send_existential_warning_title) to
                resourceManager.getString(R.string.wallet_send_existential_warnining_transfer_dust)
        }
    }
}
