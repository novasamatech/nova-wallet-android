package io.novafoundation.nova.feature_xcm_api.config.model

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
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
