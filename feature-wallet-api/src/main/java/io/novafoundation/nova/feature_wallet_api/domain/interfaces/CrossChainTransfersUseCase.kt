package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncomingDirection(
    val asset: Asset,
    val chain: Chain
)

typealias OutcomingDirection = ChainWithAsset

interface CrossChainTransfersUseCase {

    fun incomingCrossChainDirections(destination: Flow<Chain.Asset?>): Flow<List<IncomingDirection>>

    fun outcomingCrossChainDirections(origin: Chain.Asset): Flow<List<OutcomingDirection>>
}

fun CrossChainTransfersUseCase.incomingCrossChainDirectionsAvailable(destination: Flow<Chain.Asset?>): Flow<Boolean> {
    return incomingCrossChainDirections(destination).map { it.isNotEmpty() }
}
