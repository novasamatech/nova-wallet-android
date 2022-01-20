package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.StatemineBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine.StatemineAssetTransfers
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class StatemineAssetsModule {

    @Provides
    @FeatureScope
    fun provideStatemineBalanceSource(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        eventsRepository: EventsRepository,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorage: StorageDataSource
    ) = StatemineBalanceSource(chainRegistry, assetCache, eventsRepository, remoteStorage)

    @Provides
    @FeatureScope
    fun provideStatemineAssetTransfers(
        statemineBalanceSource: StatemineBalanceSource,
        balanceSourceProvider: BalanceSourceProvider,
        extrinsicService: ExtrinsicService
    ) = StatemineAssetTransfers(statemineBalanceSource, balanceSourceProvider, extrinsicService)
}
