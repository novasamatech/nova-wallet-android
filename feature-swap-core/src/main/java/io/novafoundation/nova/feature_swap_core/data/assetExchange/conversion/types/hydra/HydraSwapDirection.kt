package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface HydraSwapDirection {

    val from: FullChainAssetId

    val to: FullChainAssetId

    val params: Map<String, String>
}
