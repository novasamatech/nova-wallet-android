package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.source.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.source.NativeBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.source.StatemineBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.source.UnsupportedBalanceSource
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class BalanceSourceModule {

    @Provides
    @FeatureScope
    fun provideNativeBalanceSource(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        substrateRemoteSource: SubstrateRemoteSource,
    ) = NativeBalanceSource(chainRegistry, assetCache, substrateRemoteSource)

    @Provides
    @FeatureScope
    fun provideStatemineBalanceSource(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
    ) = StatemineBalanceSource(chainRegistry, assetCache)

    @Provides
    @FeatureScope
    fun provideUnsupportedBalanceSource() = UnsupportedBalanceSource()

    @Provides
    @FeatureScope
    fun provideBalanceSourceProvider(
        nativeBalanceSource: NativeBalanceSource,
        statemineBalanceSource: StatemineBalanceSource,
        unsupportedBalanceSource: UnsupportedBalanceSource,
    ) = BalanceSourceProvider(nativeBalanceSource, statemineBalanceSource, unsupportedBalanceSource)
}
