package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets

import dagger.Lazy
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class StaticAssetSource(
    override val transfers: AssetTransfers,
    override val balance: AssetBalance,
    override val history: AssetHistory
) : AssetSource

// Use lazy to resolve possible circular dependencies
class TypeBasedAssetSourceRegistry(
    private val nativeSource: Lazy<AssetSource>,
    private val statemineSource: Lazy<AssetSource>,
    private val ormlSource: Lazy<AssetSource>,
    private val unsupportedBalanceSource: AssetSource,
) : AssetSourceRegistry {

    override fun sourceFor(chainAsset: Chain.Asset): AssetSource {
        return when (chainAsset.type) {
            is Chain.Asset.Type.Native -> nativeSource.get()
            is Chain.Asset.Type.Statemine -> statemineSource.get()
            is Chain.Asset.Type.Orml -> ormlSource.get()
            Chain.Asset.Type.Unsupported -> unsupportedBalanceSource
        }
    }
}
