package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface ChainAssetRepository {

    suspend fun setAssetsEnabled(enabled: Boolean, assetIds: List<FullChainAssetId>)

    suspend fun insertCustomAsset(chainAsset: Chain.Asset)

    suspend fun getEnabledAssets(): List<Chain.Asset>
}
