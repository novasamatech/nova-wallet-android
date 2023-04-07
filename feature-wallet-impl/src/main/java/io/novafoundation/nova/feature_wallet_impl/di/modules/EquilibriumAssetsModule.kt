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
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.StaticAssetSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.equilibrium.EquilibriumAssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.equilibrium.EquilibriumAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.equilibrium.EquilibriumAssetTransfers
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
        extrinsicService: ExtrinsicService,
        phishingValidationFactory: PhishingValidationFactory,
        @Named(REMOTE_STORAGE_SOURCE)
        remoteStorageSource: StorageDataSource
    ) = EquilibriumAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicService, phishingValidationFactory, remoteStorageSource)

    @Provides
    @FeatureScope
    fun provideHistory() = EquilibriumAssetHistory()

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
