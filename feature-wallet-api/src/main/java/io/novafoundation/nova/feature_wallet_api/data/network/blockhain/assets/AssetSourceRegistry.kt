package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.AssetEventDetector
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AssetSourceRegistry {

    fun sourceFor(chainAsset: Chain.Asset): AssetSource

    suspend fun getEventDetector(chainAsset: Chain.Asset): AssetEventDetector
}
