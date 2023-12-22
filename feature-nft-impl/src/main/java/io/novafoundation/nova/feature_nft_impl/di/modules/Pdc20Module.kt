package io.novafoundation.nova.feature_nft_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20.Pdc20Provider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20.network.Pdc20Api
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class Pdc20Module {

    @Provides
    @FeatureScope
    fun provideApi(networkApiCreator: NetworkApiCreator): Pdc20Api {
        return networkApiCreator.create(Pdc20Api::class.java)
    }

    @Provides
    @FeatureScope
    fun provideNftProvider(
        api: Pdc20Api,
        nftDao: NftDao,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
    ) = Pdc20Provider(
        api = api,
        nftDao = nftDao,
        accountRepository = accountRepository,
        chainRegistry = chainRegistry
    )
}
