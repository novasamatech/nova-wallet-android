package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry

class RecipientCanAcceptTransferValidation(
    private val assetSourceRegistry: AssetSourceRegistry,
) : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        val destinationAsset = value.transfer.destinationChainAsset
        val source = assetSourceRegistry.sourceFor(destinationAsset)

        val recipientAccountId = value.transfer.recipientOrNull() ?: return valid()

        return source.transfers.recipientCanAcceptTransfer(destinationAsset, recipientAccountId) isTrueOrError {
            AssetTransferValidationFailure.RecipientCannotAcceptTransfer
        }
    }
}

fun AssetTransfersValidationSystemBuilder.recipientCanAcceptTransfer(
    assetSourceRegistry: AssetSourceRegistry
) {
    validate(RecipientCanAcceptTransferValidation(assetSourceRegistry))
}
