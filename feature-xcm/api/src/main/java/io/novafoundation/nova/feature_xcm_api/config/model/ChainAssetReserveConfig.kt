package io.novafoundation.nova.feature_xcm_api.config.model

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class ChainAssetReserveConfig(
    val reserveId: ChainAssetReserveId,
    val reserveAssetId: FullChainAssetId,
    val reserveLocation: AbsoluteMultiLocation,
)
