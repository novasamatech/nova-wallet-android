package io.novafoundation.nova.feature_xcm_api.config.model

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.normalizeSymbol
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class GeneralXcmConfig(
    val chains: ChainXcmConfig,
    val assets: AssetsXcmConfig
)

class ChainXcmConfig(
    val parachainIds: Map<ChainId, ParaId>
)

class AssetsXcmConfig(
    val reservesById: Map<ChainAssetReserveId, ChainAssetReserveConfig>,

    // By default, asset reserve id is equal to its symbol
    // This mapping allows to override that for cases like multiple reserves (Statemine & Polkadot for DOT)
    val assetToReserveIdOverrides: Map<FullChainAssetId, ChainAssetReserveId>,
)

fun AssetsXcmConfig.getReserve(chainAsset: Chain.Asset): ChainAssetReserveConfig {
    val reserveId = getReserveId(chainAsset)
    val reserve = reservesById.getValue(reserveId)
    return reserve
}

private fun AssetsXcmConfig.getReserveId(chainAsset: Chain.Asset): ChainAssetReserveId {
    return assetToReserveIdOverrides[chainAsset.fullId] ?: chainAsset.normalizeSymbol()
}
