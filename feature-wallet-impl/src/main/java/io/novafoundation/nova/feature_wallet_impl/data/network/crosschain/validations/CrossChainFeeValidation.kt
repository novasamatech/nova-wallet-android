package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.data.model.decimalAmountByExecutingAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.originFeeListInUsedAsset

class CrossChainFeeValidation : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        val originFeeSum = value.originFeeListInUsedAsset.sumOf { it.decimalAmountByExecutingAccount }

        val remainingBalanceAfterTransfer = value.originUsedAsset.transferable - value.transfer.amount - originFeeSum

        val crossChainFee = value.crossChainFee?.decimalAmount.orZero()
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
