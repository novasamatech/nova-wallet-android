package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets

import dagger.Lazy
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.AssetEventDetector
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.UnsupportedEventDetector
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.orml.OrmlAssetEventDetectorFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.utility.NativeAssetEventDetector
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class StaticAssetSource(
    override val transfers: AssetTransfers,
    override val balance: AssetBalance,
    override val history: AssetHistory,
) : AssetSource

// Use lazy to resolve possible circular dependencies
class TypeBasedAssetSourceRegistry(
    private val nativeSource: Lazy<AssetSource>,
    private val statemineSource: Lazy<AssetSource>,
    private val ormlSource: Lazy<AssetSource>,
    private val evmErc20Source: Lazy<AssetSource>,
    private val evmNativeSource: Lazy<AssetSource>,
    private val equilibriumAssetSource: Lazy<AssetSource>,
    private val unsupportedBalanceSource: AssetSource,

    private val nativeAssetEventDetector: NativeAssetEventDetector,
    private val ormlAssetEventDetectorFactory: OrmlAssetEventDetectorFactory
) : AssetSourceRegistry {

    override fun sourceFor(chainAsset: Chain.Asset): AssetSource {
        return when (chainAsset.type) {
            is Chain.Asset.Type.Native -> nativeSource.get()
            is Chain.Asset.Type.Statemine -> statemineSource.get()
            is Chain.Asset.Type.Orml -> ormlSource.get()
            is Chain.Asset.Type.EvmErc20 -> evmErc20Source.get()
            is Chain.Asset.Type.EvmNative -> evmNativeSource.get()
            is Chain.Asset.Type.Equilibrium -> equilibriumAssetSource.get()
            Chain.Asset.Type.Unsupported -> unsupportedBalanceSource
        }
    }

    override suspend fun getEventDetector(chainAsset: Chain.Asset): AssetEventDetector {
        return when (chainAsset.type) {
            is Chain.Asset.Type.Equilibrium,
            is Chain.Asset.Type.EvmErc20,
            Chain.Asset.Type.EvmNative,

            // TODO implement statemine
            is Chain.Asset.Type.Statemine,
            Chain.Asset.Type.Unsupported -> UnsupportedEventDetector()

            is Chain.Asset.Type.Orml -> ormlAssetEventDetectorFactory.create(chainAsset)

            Chain.Asset.Type.Native -> nativeAssetEventDetector
        }
    }
}
