package io.novafoundation.nova.feature_nft_impl.domain.validaiton

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.recipientOrNull

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
