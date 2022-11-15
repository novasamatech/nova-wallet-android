package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.TypeBasedAssetSourceRegistry

@Module(
    includes = [
        NativeAssetsModule::class,
        StatemineAssetsModule::class,
        OrmlAssetsModule::class,
        EvmAssetsModule::class,
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
        @EvmAssets evm: Lazy<AssetSource>,
        @UnsupportedAssets unsupported: AssetSource,
    ): AssetSourceRegistry = TypeBasedAssetSourceRegistry(
        nativeSource = native,
        statemineSource = statemine,
        ormlSource = orml,
        evmSource = evm,
        unsupportedBalanceSource = unsupported
    )
}
