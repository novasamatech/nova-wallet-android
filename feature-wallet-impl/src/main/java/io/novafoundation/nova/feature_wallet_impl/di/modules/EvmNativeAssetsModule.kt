package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.evnNative.EvmNativeAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evmNative.EvmNativeAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.evmNative.EvmNativeAssetTransfers
import javax.inject.Qualifier

@Qualifier
annotation class EvmNativeAssets

@Module
class EvmNativeAssetsModule {

    @Provides
    @FeatureScope
    fun provideBalance(
        assetCache: AssetCache,
    ) = EvmNativeAssetBalance(assetCache)

    @Provides
    @FeatureScope
    fun provideTransfers() = EvmNativeAssetTransfers()

    @Provides
    @FeatureScope
    fun provideHistory() = EvmNativeAssetHistory()

    @Provides
    @EvmNativeAssets
    @FeatureScope
    fun provideAssetSource(
        evmNativeAssetBalance: EvmNativeAssetBalance,
        evmNativeAssetTransfers: EvmNativeAssetTransfers,
        evmNativeAssetHistory: EvmNativeAssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = evmNativeAssetTransfers,
        balance = evmNativeAssetBalance,
        history = evmNativeAssetHistory
    )
}
