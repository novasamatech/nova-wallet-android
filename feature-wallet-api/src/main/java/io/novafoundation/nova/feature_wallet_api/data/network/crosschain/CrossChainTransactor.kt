package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration

interface CrossChainTransactor {

    val validationSystem: AssetTransfersValidationSystem

    suspend fun ExtrinsicService.estimateOriginFee(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase
    ): Fee

    suspend fun ExtrinsicService.performTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): Result<ExtrinsicSubmission>
}
