package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferFeatures
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlin.time.Duration

interface CrossChainTransactor {

    context(ExtrinsicService)
    suspend fun estimateOriginFee(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase
    ): Fee

    context(ExtrinsicService)
    suspend fun performTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): Result<ExtrinsicSubmission>

    suspend fun requiredRemainingAmountAfterTransfer(configuration: CrossChainTransferConfiguration): Balance

    /**
     * @return result of actual received balance on destination
     */
    context(ExtrinsicService)
    suspend fun performAndTrackTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
    ): Result<Balance>

    suspend fun supportsXcmExecute(
        originChainId: ChainId,
        features: DynamicCrossChainTransferFeatures
    ): Boolean

    suspend fun estimateMaximumExecutionTime(configuration: CrossChainTransferConfiguration): Duration
}
