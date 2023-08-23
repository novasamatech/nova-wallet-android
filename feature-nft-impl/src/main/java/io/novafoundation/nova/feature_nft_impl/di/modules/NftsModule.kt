package io.novafoundation.nova.feature_nft_impl.di.modules

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_impl.data.source.providers.nfts.NftsNftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.network.IpfsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class NftsModule {

    @Provides
    @FeatureScope
    fun provideNftsNftProvider(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        nftDao: NftDao,
        ipfsApi: IpfsApi,
        gson: Gson
    ) = NftsNftProvider(remoteStorageSource, accountRepository, chainRegistry, nftDao, ipfsApi, gson)
}
