package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility.NativeAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.utility.NativeAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility.NativeAssetTransfers
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
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
        lockDao: LockDao
    ) = NativeAssetBalance(chainRegistry, assetCache, substrateRemoteSource, lockDao)

    @Provides
    @FeatureScope
    fun provideTransfers(
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
        extrinsicService: ExtrinsicService,
        phishingValidationFactory: PhishingValidationFactory,
    ) = NativeAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicService, phishingValidationFactory)

    @Provides
    @FeatureScope
    fun provideHistory(
        chainRegistry: ChainRegistry,
        eventsRepository: EventsRepository,
        subQueryOperationsApi: SubQueryOperationsApi,
        cursorStorage: TransferCursorStorage
    ) = NativeAssetHistory(chainRegistry, eventsRepository, subQueryOperationsApi, cursorStorage)

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
