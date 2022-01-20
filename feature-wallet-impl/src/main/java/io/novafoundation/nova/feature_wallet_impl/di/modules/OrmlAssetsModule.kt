package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml.OrmlBalanceSource
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class OrmlAssetsModule {

    @Provides
    @FeatureScope
    fun provideStatemineBalanceSource(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
    ) = OrmlBalanceSource(assetCache, chainRegistry)
}
