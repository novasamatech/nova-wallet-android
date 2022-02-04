package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml.OrmlAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine.StatemineAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility.NativeAssetTransfers
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class AssetTransfersProvider(
    private val nativeAssetTransfers: NativeAssetTransfers,
    private val statemineAssetTransfers: StatemineAssetTransfers,
    private val ormlAssetTransfers: OrmlAssetTransfers,
) {

    fun provideFor(chainAsset: Chain.Asset): AssetTransfers = when (chainAsset.type) {
        Chain.Asset.Type.Native -> nativeAssetTransfers
        is Chain.Asset.Type.Statemine -> statemineAssetTransfers
        is Chain.Asset.Type.Orml -> ormlAssetTransfers
        Chain.Asset.Type.Unsupported -> throw UnsupportedOperationException("Unsupported asset")
    }
}
