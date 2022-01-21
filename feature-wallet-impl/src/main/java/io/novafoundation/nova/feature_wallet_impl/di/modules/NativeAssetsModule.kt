package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility.NativeBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility.NativeAssetTransfers
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository

@Module
class NativeAssetsModule {

    @Provides
    @FeatureScope
    fun provideNativeBalanceSource(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        eventsRepository: EventsRepository,
        substrateRemoteSource: SubstrateRemoteSource,
    ) = NativeBalanceSource(chainRegistry, assetCache, eventsRepository, substrateRemoteSource)

    @Provides
    @FeatureScope
    fun provideNativeAssetTransfers(
        chainRegistry: ChainRegistry,
        balanceSourceProvider: BalanceSourceProvider,
        extrinsicService: ExtrinsicService
    ) = NativeAssetTransfers(chainRegistry, balanceSourceProvider, extrinsicService)
}
