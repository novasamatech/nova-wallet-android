package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface ChainAssetRepository {

    suspend fun setAssetsEnabled(enabled: Boolean, assetIds: List<FullChainAssetId>)
}
