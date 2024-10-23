package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.TypeBasedAssetSourceRegistry
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.orml.OrmlAssetEventDetectorFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.statemine.StatemineAssetEventDetectorFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.utility.NativeAssetEventDetector

@Module(
    includes = [
        NativeAssetsModule::class,
        StatemineAssetsModule::class,
        OrmlAssetsModule::class,
        EvmErc20AssetsModule::class,
        EvmNativeAssetsModule::class,
        EquilibriumAssetsModule::class,
        UnsupportedAssetsModule::class
    ]
)
class AssetsModule {

    @Provides
    @FeatureScope
    fun provideAssetSourceRegistry(
        @NativeAsset native: Lazy<AssetSource>,
        @StatemineAssets statemine: Lazy<AssetSource>,
        @OrmlAssets orml: Lazy<AssetSource>,
        @EvmErc20Assets evmErc20: Lazy<AssetSource>,
        @EvmNativeAssets evmNative: Lazy<AssetSource>,
        @EquilibriumAsset equilibrium: Lazy<AssetSource>,
        @UnsupportedAssets unsupported: AssetSource,

        nativeAssetEventDetector: NativeAssetEventDetector,
        ormlAssetEventDetectorFactory: OrmlAssetEventDetectorFactory,
        statemineAssetEventDetectorFactory: StatemineAssetEventDetectorFactory,
    ): AssetSourceRegistry = TypeBasedAssetSourceRegistry(
        nativeSource = native,
        statemineSource = statemine,
        ormlSource = orml,
        evmErc20Source = evmErc20,
        evmNativeSource = evmNative,
        equilibriumAssetSource = equilibrium,
        unsupportedBalanceSource = unsupported,

        nativeAssetEventDetector = nativeAssetEventDetector,
        ormlAssetEventDetectorFactory = ormlAssetEventDetectorFactory,
        statemineAssetEventDetectorFactory = statemineAssetEventDetectorFactory
    )
}
