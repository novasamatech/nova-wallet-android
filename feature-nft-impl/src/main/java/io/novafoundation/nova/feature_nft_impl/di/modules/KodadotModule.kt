package io.novafoundation.nova.feature_nft_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.KodadotProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.KodadotApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class KodadotModule {

    @Provides
    @FeatureScope
    fun provideApi(networkApiCreator: NetworkApiCreator): KodadotApi {
        return networkApiCreator.create(KodadotApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideKodadotProvider(
        api: KodadotApi,
        nftDao: NftDao,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
    ) = KodadotProvider(
        api = api,
        nftDao = nftDao,
        accountRepository = accountRepository,
        chainRegistry = chainRegistry
    )
}
