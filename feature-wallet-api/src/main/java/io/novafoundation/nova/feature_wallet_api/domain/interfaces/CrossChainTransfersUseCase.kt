package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.XcmTransferDryRunOrigin
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferFeatures
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

class IncomingDirection(
    val asset: Asset,
    val chain: Chain
)

typealias OutcomingDirection = ChainWithAsset

interface CrossChainTransfersUseCase {

    suspend fun syncCrossChainConfig()

    fun incomingCrossChainDirections(destination: Flow<Chain.Asset?>): Flow<List<IncomingDirection>>

    fun outcomingCrossChainDirectionsFlow(origin: Chain.Asset): Flow<List<OutcomingDirection>>

    suspend fun getConfiguration(): CrossChainTransfersConfiguration

    suspend fun transferConfigurationFor(transfer: AssetTransferDirection, cachingScope: CoroutineScope?): CrossChainTransferConfiguration

    suspend fun requiredRemainingAmountAfterTransfer(
        originChain: Chain,
        sendingAsset: Chain.Asset,
        destinationChain: Chain,
    ): Balance

    /**
     * @param cachingScope - a scope that will be registered as a dependency for internal caching. If null is passed, no caching will be used
     */
    suspend fun ExtrinsicService.estimateFee(
        transfer: AssetTransferBase,
        cachingScope: CoroutineScope?
    ): CrossChainTransferFee

    suspend fun ExtrinsicService.performTransferOfExactAmount(transfer: AssetTransferBase, computationalScope: CoroutineScope): Result<ExtrinsicSubmission>

    /**
     * @return result of actual received balance on destination
     */
    suspend fun ExtrinsicService.performTransferAndTrackTransfer(
        transfer: AssetTransferBase,
        computationalScope: CoroutineScope
    ): Result<Balance>

    suspend fun maximumExecutionTime(
        assetTransferDirection: AssetTransferDirection,
        computationalScope: CoroutineScope
    ): Duration

    suspend fun dryRunTransferIfPossible(
        transfer: AssetTransferBase,
        origin: XcmTransferDryRunOrigin,
        computationalScope: CoroutineScope
    ): Result<Unit>

    suspend fun supportsXcmExecute(
        originChainId: ChainId,
        features: DynamicCrossChainTransferFeatures
    ): Boolean
}

fun CrossChainTransfersUseCase.incomingCrossChainDirectionsAvailable(destination: Flow<Chain.Asset?>): Flow<Boolean> {
    return incomingCrossChainDirections(destination).map { it.isNotEmpty() }
}
