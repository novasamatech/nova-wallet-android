package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainFee
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration

interface CrossChainTransactor {

    val validationSystem: AssetTransfersValidationSystem

    suspend fun estimateOriginFee(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransfer
    ): Fee

    suspend fun performTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransfer,
        crossChainFee: CrossChainFee
    ): Result<ExtrinsicSubmission>
}
