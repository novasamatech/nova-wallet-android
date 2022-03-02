package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AssetTransfersProvider {

    fun provideFor(chainAsset: Chain.Asset): AssetTransfers
}
