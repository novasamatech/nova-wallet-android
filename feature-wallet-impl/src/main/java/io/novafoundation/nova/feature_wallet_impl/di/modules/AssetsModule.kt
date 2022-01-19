package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.UnsupportedBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.StatemineBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility.NativeBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.AssetTransfersProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine.StatemineAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility.NativeAssetTransfers

@Module(
    includes = [
        NativeAssetsModule::class,
        StatemineAssetsModule::class
    ]
)
class AssetsModule {

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

    @Provides
    @FeatureScope
    fun provideAssetTransfersProvider(
        nativeAssetTransfers: NativeAssetTransfers,
        statemineAssetTransfers: StatemineAssetTransfers,
    ) = AssetTransfersProvider(nativeAssetTransfers, statemineAssetTransfers)
}
