package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferFee
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
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

    suspend fun allDirections(): List<Edge<FullChainAssetId>>

    suspend fun ExtrinsicService.estimateFee(
        transfer: AssetTransferBase,
        computationalScope: CoroutineScope
    ): CrossChainTransferFee
}

fun CrossChainTransfersUseCase.incomingCrossChainDirectionsAvailable(destination: Flow<Chain.Asset?>): Flow<Boolean> {
    return incomingCrossChainDirections(destination).map { it.isNotEmpty() }
}
