package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.StatemineAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.events.statemine.StatemineAssetEventDetectorFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.statemine.StatemineAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine.StatemineAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
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
        @Named(REMOTE_STORAGE_SOURCE) remoteStorage: StorageDataSource,
        @Named(LOCAL_STORAGE_SOURCE) localStorage: StorageDataSource,
        storageCache: StorageCache
    ) = StatemineAssetBalance(
        chainRegistry = chainRegistry,
        assetCache = assetCache,
        remoteStorage = remoteStorage,
        localStorage = localStorage,
        storageCache = storageCache
    )

    @Provides
    @FeatureScope
    fun provideTransfers(
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
        extrinsicServiceFactory: ExtrinsicService.Factory,
        phishingValidationFactory: PhishingValidationFactory,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorage: StorageDataSource,
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ) = StatemineAssetTransfers(
        chainRegistry,
        assetSourceRegistry,
        extrinsicServiceFactory,
        phishingValidationFactory,
        enoughTotalToStayAboveEDValidationFactory,
        remoteStorage
    )

    @Provides
    @FeatureScope
    fun provideHistory(
        chainRegistry: ChainRegistry,
        realtimeOperationFetcherFactory: SubstrateRealtimeOperationFetcher.Factory,
        subQueryOperationsApi: SubQueryOperationsApi,
        cursorStorage: TransferCursorStorage,
        coinPriceRepository: CoinPriceRepository
    ) = StatemineAssetHistory(
        chainRegistry = chainRegistry,
        realtimeOperationFetcherFactory = realtimeOperationFetcherFactory,
        walletOperationsApi = subQueryOperationsApi,
        cursorStorage = cursorStorage,
        coinPriceRepository = coinPriceRepository
    )

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

    @Provides
    @FeatureScope
    fun provideStatemineAssetEventDetectorFactory(chainRegistry: ChainRegistry) = StatemineAssetEventDetectorFactory(chainRegistry)
}
