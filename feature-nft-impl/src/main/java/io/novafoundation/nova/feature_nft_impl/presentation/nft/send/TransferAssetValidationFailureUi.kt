package io.novafoundation.nova.feature_nft_impl.presentation.nft.send

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError

fun mapNftTransferValidationFailureToUI(
    resourceManager: ResourceManager,
    failure: NftTransferValidationFailure
): TitleAndMessage {
    return when (failure) {
        NftTransferValidationFailure.NotEnoughFunds.InUsedAsset -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.choose_amount_error_too_big)
        }
        is NftTransferValidationFailure.NotEnoughFunds.InCommissionAsset -> {
            handleNotEnoughFeeError(failure, resourceManager)
        }
        is NftTransferValidationFailure.InvalidRecipientAddress -> {
            resourceManager.getString(R.string.common_validation_invalid_address_title) to
                resourceManager.getString(R.string.common_validation_invalid_address_message, failure.chain.name)
        }
        is NftTransferValidationFailure.PhishingRecipient -> {
            resourceManager.getString(R.string.wallet_send_phishing_warning_title) to
                resourceManager.getString(R.string.wallet_send_phishing_warning_text, failure.address)
        }
        is NftTransferValidationFailure.NotEnoughFunds.ToStayAboveED -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.wallet_send_insufficient_balance_commission, failure.commissionAsset.symbol)
        }

        NftTransferValidationFailure.NftAbsent -> {
            resourceManager.getString(R.string.common_nft_absent_title) to
                resourceManager.getString(R.string.common_nft_absent_message)
        }
    }
}
