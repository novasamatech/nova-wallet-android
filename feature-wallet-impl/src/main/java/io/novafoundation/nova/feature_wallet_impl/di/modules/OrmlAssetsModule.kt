package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml.OrmlBalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml.OrmlAssetTransfers
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class OrmlAssetsModule {

    @Provides
    @FeatureScope
    fun provideOrmlBalanceSource(
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource,
        eventsRepository: EventsRepository,
        assetCache: AssetCache,
    ) = OrmlBalanceSource(assetCache, storageDataSource, chainRegistry, eventsRepository)

    @Provides
    @FeatureScope
    fun provideOrmlAssetTransfers(
        chainRegistry: ChainRegistry,
        balanceSourceProvider: BalanceSourceProvider,
        extrinsicService: ExtrinsicService,
        phishingValidationFactory: PhishingValidationFactory,
    ) = OrmlAssetTransfers(chainRegistry, balanceSourceProvider, extrinsicService, phishingValidationFactory)
}
