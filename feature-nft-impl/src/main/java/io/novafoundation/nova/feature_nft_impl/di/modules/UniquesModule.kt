package io.novafoundation.nova.feature_nft_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.UniquesNftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.network.IpfsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class UniquesModule {

    @Provides
    @FeatureScope
    fun provideIpfsApi(networkApiCreator: NetworkApiCreator) = networkApiCreator.create(IpfsApi::class.java)

    @Provides
    @FeatureScope
    fun provideUniquesNftProvider(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        nftDao: NftDao,
        ipfsApi: IpfsApi,
    ) = UniquesNftProvider(remoteStorageSource, accountRepository, chainRegistry, nftDao, ipfsApi)
}
