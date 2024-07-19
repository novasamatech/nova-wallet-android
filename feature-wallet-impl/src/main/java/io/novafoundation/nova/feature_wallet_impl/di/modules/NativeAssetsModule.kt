package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.HoldsDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility.NativeAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.utility.NativeAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility.NativeAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Qualifier
annotation class NativeAsset

@Module
class NativeAssetsModule {

    @Provides
    @FeatureScope
    fun provideBalance(
        chainRegistry: ChainRegistry,
        assetCache: AssetCache,
        substrateRemoteSource: SubstrateRemoteSource,
        @Named(REMOTE_STORAGE_SOURCE) remoteSource: StorageDataSource,
        lockDao: LockDao,
        holdsDao: HoldsDao,
    ) = NativeAssetBalance(
        chainRegistry = chainRegistry,
        assetCache = assetCache,
        substrateRemoteSource = substrateRemoteSource,
        remoteStorage = remoteSource,
        lockDao = lockDao,
        holdsDao = holdsDao
    )

    @Provides
    @FeatureScope
    fun provideTransfers(
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
        extrinsicService: ExtrinsicService,
        phishingValidationFactory: PhishingValidationFactory,
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource,
        accountRepository: AccountRepository,
    ) = NativeAssetTransfers(
        chainRegistry,
        assetSourceRegistry,
        extrinsicService,
        phishingValidationFactory,
        enoughTotalToStayAboveEDValidationFactory,
        storageDataSource,
        accountRepository
    )

    @Provides
    @FeatureScope
    fun provideHistory(
        chainRegistry: ChainRegistry,
        realtimeOperationFetcherFactory: SubstrateRealtimeOperationFetcher.Factory,
        subQueryOperationsApi: SubQueryOperationsApi,
        cursorStorage: TransferCursorStorage,
        coinPriceRepository: CoinPriceRepository
    ) = NativeAssetHistory(
        chainRegistry = chainRegistry,
        realtimeOperationFetcherFactory = realtimeOperationFetcherFactory,
        walletOperationsApi = subQueryOperationsApi,
        cursorStorage = cursorStorage,
        coinPriceRepository = coinPriceRepository
    )

    @Provides
    @NativeAsset
    @FeatureScope
    fun provideAssetSource(
        nativeAssetBalance: NativeAssetBalance,
        nativeAssetTransfers: NativeAssetTransfers,
        nativeAssetHistory: NativeAssetHistory,
    ): AssetSource = StaticAssetSource(
        transfers = nativeAssetTransfers,
        balance = nativeAssetBalance,
        history = nativeAssetHistory
    )
}
