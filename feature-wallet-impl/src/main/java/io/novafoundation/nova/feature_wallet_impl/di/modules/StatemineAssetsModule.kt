package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.StatemineAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.statemine.StatemineAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine.StatemineAssetTransfers
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
annotation class StatemineAssets

@Module
class StatemineAssetsModule {

    @Provides
    @FeatureScope
    fun provideBalance(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource,
    ) = StatemineAssetBalance(chainRegistry, assetCache, storageDataSource)

    @Provides
    @FeatureScope
    fun provideTransfers(
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
        extrinsicService: ExtrinsicService,
        phishingValidationFactory: PhishingValidationFactory,
    ) = StatemineAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicService, phishingValidationFactory)

    @Provides
    @FeatureScope
    fun provideHistory(
        chainRegistry: ChainRegistry,
        eventsRepository: EventsRepository,
    ) = StatemineAssetHistory(chainRegistry, eventsRepository)

    @Provides
    @StatemineAssets
    @FeatureScope
    fun provideAssetSource(
        statemineAssetBalance: StatemineAssetBalance,
        statemineAssetTransfers: StatemineAssetTransfers,
        statemineAssetHistory: StatemineAssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = statemineAssetTransfers,
        balance = statemineAssetBalance,
        history = statemineAssetHistory
    )
}
