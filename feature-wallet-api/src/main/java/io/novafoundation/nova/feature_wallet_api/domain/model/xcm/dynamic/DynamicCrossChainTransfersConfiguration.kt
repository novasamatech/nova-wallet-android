package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class DynamicCrossChainTransfersConfiguration(
    val reserveRegistry: TokenReserveRegistry,
    val chains: Map<ChainId, List<AssetTransfers>>
) {

    class AssetTransfers(
        val assetId: ChainAssetId,
        val destinations: List<TransferDestination>
    )

    class TransferDestination(
        val fullChainAssetId: FullChainAssetId,
        val hasDeliveryFee: Boolean
    )
}


