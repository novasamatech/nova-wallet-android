package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.UnsupportedAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.UnsupportedAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.UnsupportedAssetTransfers
import javax.inject.Qualifier

@Qualifier
annotation class UnsupportedAssets

@Module
class UnsupportedAssetsModule {

    @Provides
    @FeatureScope
    fun provideBalance() = UnsupportedAssetBalance()

    @Provides
    @FeatureScope
    fun provideTransfers() = UnsupportedAssetTransfers()

    @Provides
    @FeatureScope
    fun provideHistory() = UnsupportedAssetHistory()

    @Provides
    @FeatureScope
    @UnsupportedAssets
    fun provideSource(
        unsupportedAssetBalance: UnsupportedAssetBalance,
        unsupportedAssetTransfers: UnsupportedAssetTransfers,
        unsupportedAssetHistory: UnsupportedAssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = unsupportedAssetTransfers,
        balance = unsupportedAssetBalance,
        history = unsupportedAssetHistory
    )
}
