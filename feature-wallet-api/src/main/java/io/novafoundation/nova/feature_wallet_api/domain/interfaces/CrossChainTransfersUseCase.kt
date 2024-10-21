package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferFee
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    suspend fun ExtrinsicService.estimateFee(
        transfer: AssetTransferBase,
        computationalScope: CoroutineScope
    ): CrossChainTransferFee

    /**
     * @return result of actual received balance on destination
     */
    suspend fun ExtrinsicService.performTransfer(
        transfer: AssetTransferBase,
        computationalScope: CoroutineScope
    ): Result<Balance>
}

fun CrossChainTransfersUseCase.incomingCrossChainDirectionsAvailable(destination: Flow<Chain.Asset?>): Flow<Boolean> {
    return incomingCrossChainDirections(destination).map { it.isNotEmpty() }
}
