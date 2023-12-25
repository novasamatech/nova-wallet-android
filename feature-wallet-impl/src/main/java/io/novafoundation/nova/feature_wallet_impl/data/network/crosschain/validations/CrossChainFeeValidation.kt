package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.originFeeInUsedAsset
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeByRequestedAccountOrZero

class CrossChainFeeValidation : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        val networkFeeInUsedAsset = value.originFeeInUsedAsset.networkFeeByRequestedAccountOrZero
        val remainingBalanceAfterTransfer = value.originUsedAsset.transferable - value.transfer.amount - networkFeeInUsedAsset

        val crossChainFee = value.crossChainFee.networkFeeByRequestedAccountOrZero
        val remainsEnoughToPayCrossChainFees = remainingBalanceAfterTransfer >= crossChainFee

        return remainsEnoughToPayCrossChainFees isTrueOrError {
            AssetTransferValidationFailure.NotEnoughFunds.ToPayCrossChainFee(
                usedAsset = value.transfer.originChainAsset,
                fee = crossChainFee,
                remainingBalanceAfterTransfer = remainingBalanceAfterTransfer
            )
        }
    }
}

fun AssetTransfersValidationSystemBuilder.canPayCrossChainFee() = validate(CrossChainFeeValidation())
