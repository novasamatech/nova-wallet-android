package io.novafoundation.nova.feature_nft_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.RmrkV1NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.network.RmrkV1Api
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class RmrkV1Module {

    @Provides
    @FeatureScope
    fun provideApi(networkApiCreator: NetworkApiCreator): RmrkV1Api {
        return networkApiCreator.create(RmrkV1Api::class.java, RmrkV1Api.BASE_URL)
    }

    @Provides
    @FeatureScope
    fun provideNftProvider(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        api: RmrkV1Api,
        nftDao: NftDao
    ) = RmrkV1NftProvider(chainRegistry, accountRepository, api, nftDao)
}
