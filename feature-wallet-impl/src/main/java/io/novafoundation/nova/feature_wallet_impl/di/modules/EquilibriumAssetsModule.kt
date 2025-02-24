package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.equilibrium.EquilibriumAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.equilibrium.EquilibriumAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.equilibrium.EquilibriumAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
annotation class EquilibriumAsset

@Module
class EquilibriumAssetsModule {

    @Provides
    @FeatureScope
    fun provideBalance(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        lockDao: LockDao,
        assetDao: AssetDao,
        @Named(REMOTE_STORAGE_SOURCE)
        remoteStorageSource: StorageDataSource
    ) = EquilibriumAssetBalance(chainRegistry, assetCache, lockDao, assetDao, remoteStorageSource)

    @Provides
    @FeatureScope
    fun provideTransfers(
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
        extrinsicServiceFactory: ExtrinsicService.Factory,
        phishingValidationFactory: PhishingValidationFactory,
        @Named(REMOTE_STORAGE_SOURCE)
        remoteStorageSource: StorageDataSource,
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ) = EquilibriumAssetTransfers(
        chainRegistry,
        assetSourceRegistry,
        extrinsicServiceFactory,
        phishingValidationFactory,
        remoteStorageSource,
        enoughTotalToStayAboveEDValidationFactory
    )

    @Provides
    @FeatureScope
    fun provideHistory(
        chainRegistry: ChainRegistry,
        realtimeOperationFetcherFactory: SubstrateRealtimeOperationFetcher.Factory,
        subQueryOperationsApi: SubQueryOperationsApi,
        cursorStorage: TransferCursorStorage,
        coinPriceRepository: CoinPriceRepository
    ) = EquilibriumAssetHistory(
        chainRegistry = chainRegistry,
        walletOperationsApi = subQueryOperationsApi,
        cursorStorage = cursorStorage,
        coinPriceRepository = coinPriceRepository,
        realtimeOperationFetcherFactory = realtimeOperationFetcherFactory
    )

    @Provides
    @EquilibriumAsset
    @FeatureScope
    fun provideAssetSource(
        assetBalance: EquilibriumAssetBalance,
        assetTransfers: EquilibriumAssetTransfers,
        assetHistory: EquilibriumAssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = assetTransfers,
        balance = assetBalance,
        history = assetHistory
    )
}
