package io.novafoundation.nova.feature_nft_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.repository.NftRepositoryImpl
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvidersRegistry
import io.novafoundation.nova.feature_nft_impl.data.source.providers.UniquesNftProvider
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class NftFeatureModule {

    @Provides
    @FeatureScope
    fun provideUniquesNftProvider(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        nftDao: NftDao
    )= UniquesNftProvider(remoteStorageSource, nftDao)

    @Provides
    @FeatureScope
    fun provideNftProviderRegistry(
       uniquesNftProvider: UniquesNftProvider
    )= NftProvidersRegistry(uniquesNftProvider)

    @Provides
    @FeatureScope
    fun provideNftRepository(
        nftProvidersRegistry: NftProvidersRegistry,
        chainRegistry: ChainRegistry,
        nftDao: NftDao
    ): NftRepository = NftRepositoryImpl(nftProvidersRegistry, chainRegistry, nftDao)
}
