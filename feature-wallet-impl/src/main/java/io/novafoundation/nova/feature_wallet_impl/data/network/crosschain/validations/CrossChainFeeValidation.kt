package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.originFeeIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.sendingAmountIn

class CrossChainFeeValidation : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        val crossChainFee = value.crossChainFee ?: return valid()

        val crossChainFeeAsset = crossChainFee.asset.token.configuration

        val remainingBalanceAfterTransfer = crossChainFee.asset.transferable -
            value.sendingAmountIn(crossChainFeeAsset) -
            value.originFeeIn(crossChainFeeAsset)

        val crossChainFeeAmount = crossChainFee.amount.orZero()
        val remainsEnoughToPayCrossChainFees = remainingBalanceAfterTransfer >= crossChainFeeAmount

        return remainsEnoughToPayCrossChainFees isTrueOrError {
            AssetTransferValidationFailure.NotEnoughFunds.ToPayCrossChainFee(
                crossChainFeeAsset = crossChainFeeAsset,
                fee = crossChainFeeAmount,
                remainingBalanceAfterTransfer = remainingBalanceAfterTransfer
            )
        }
    }
}

fun AssetTransfersValidationSystemBuilder.canPayCrossChainFee() = validate(CrossChainFeeValidation())
