package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets

import dagger.Lazy
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.AssetEventDetector
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.common.orml.OrmlAssetSourceFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.UnsupportedEventDetector
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.evmErc20.EvmErc20EventDetectorFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.orml.OrmlAssetEventDetectorFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.statemine.StatemineAssetEventDetectorFactory
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
    private val ormlSourceFactory: Lazy<OrmlAssetSourceFactory>,
    private val evmErc20Source: Lazy<AssetSource>,
    private val evmNativeSource: Lazy<AssetSource>,
    private val equilibriumAssetSource: Lazy<AssetSource>,
    private val unsupportedBalanceSource: AssetSource,

    private val nativeAssetEventDetector: NativeAssetEventDetector,
    private val ormlAssetEventDetectorFactory: OrmlAssetEventDetectorFactory,
    private val statemineAssetEventDetectorFactory: StatemineAssetEventDetectorFactory,
    private val erc20EventDetectorFactory: EvmErc20EventDetectorFactory
) : AssetSourceRegistry {

    override fun sourceFor(chainAsset: Chain.Asset): AssetSource {
        return when (val type = chainAsset.type) {
            is Chain.Asset.Type.Native -> nativeSource.get()
            is Chain.Asset.Type.Statemine -> statemineSource.get()
            is Chain.Asset.Type.Orml -> ormlSourceFactory.get().getSourceBySubtype(type.subType)
            is Chain.Asset.Type.EvmErc20 -> evmErc20Source.get()
            is Chain.Asset.Type.EvmNative -> evmNativeSource.get()
            is Chain.Asset.Type.Equilibrium -> equilibriumAssetSource.get()
            Chain.Asset.Type.Unsupported -> unsupportedBalanceSource
        }
    }

    override fun allSources(): List<AssetSource> {
        return buildList {
            add(nativeSource.get())
            add(statemineSource.get())
            addAll(ormlSourceFactory.get().allSources())
            add(evmNativeSource.get())
            add(evmErc20Source.get())
            add(equilibriumAssetSource.get())
        }
    }

    override suspend fun getEventDetector(chainAsset: Chain.Asset): AssetEventDetector {
        return when (chainAsset.type) {
            is Chain.Asset.Type.Equilibrium,
            Chain.Asset.Type.EvmNative,

            Chain.Asset.Type.Unsupported -> UnsupportedEventDetector()

            is Chain.Asset.Type.Statemine -> statemineAssetEventDetectorFactory.create(chainAsset)

            is Chain.Asset.Type.Orml -> ormlAssetEventDetectorFactory.create(chainAsset)

            Chain.Asset.Type.Native -> nativeAssetEventDetector

            is Chain.Asset.Type.EvmErc20 -> erc20EventDetectorFactory.create(chainAsset)
        }
    }
}
