package io.novafoundation.nova.feature_nft_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.UniqueNetworkNftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.UniqueNetworkApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class UniqueNetworkModule {

    @Provides
    @FeatureScope
    fun provideApi(networkApiCreator: NetworkApiCreator): UniqueNetworkApi {
        return networkApiCreator.create(UniqueNetworkApi::class.java, UniqueNetworkApi.BASE_URL)
    }

    @Provides
    @FeatureScope
    fun provideUniqueNetworkNftProvider(
        uniqueNetworkApi: UniqueNetworkApi,
        nftDao: NftDao,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
    ) = UniqueNetworkNftProvider(
        uniqueNetworkApi = uniqueNetworkApi,
        nftDao = nftDao,
        accountRepository = accountRepository,
        chainRegistry = chainRegistry
    )
}
