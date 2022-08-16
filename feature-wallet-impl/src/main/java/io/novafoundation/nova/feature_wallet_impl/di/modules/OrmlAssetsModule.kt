package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml.OrmlAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.orml.OrmlAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml.OrmlAssetTransfers
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
annotation class OrmlAssets

@Module
class OrmlAssetsModule {

    @Provides
    @FeatureScope
    fun provideBalance(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        @Named(REMOTE_STORAGE_SOURCE) remoteDataSource: StorageDataSource,
        @Named(LOCAL_STORAGE_SOURCE) localDataSource: StorageDataSource,
        storageCache: StorageCache
    ) = OrmlAssetBalance(assetCache, remoteDataSource, localDataSource, chainRegistry, storageCache)

    @Provides
    @FeatureScope
    fun provideTransfers(
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
        extrinsicService: ExtrinsicService,
        phishingValidationFactory: PhishingValidationFactory,
    ) = OrmlAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicService, phishingValidationFactory)

    @Provides
    @FeatureScope
    fun provideHistory(
        chainRegistry: ChainRegistry,
        eventsRepository: EventsRepository,
    ) = OrmlAssetHistory(chainRegistry, eventsRepository)

    @Provides
    @OrmlAssets
    @FeatureScope
    fun provideAssetSource(
        ormlAssetBalance: OrmlAssetBalance,
        ormlAssetTransfers: OrmlAssetTransfers,
        ormlAssetHistory: OrmlAssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = ormlAssetTransfers,
        balance = ormlAssetBalance,
        history = ormlAssetHistory
    )
}
