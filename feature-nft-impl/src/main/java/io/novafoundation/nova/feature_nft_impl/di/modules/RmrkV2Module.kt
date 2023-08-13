package io.novafoundation.nova.feature_nft_impl.di.modules

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.RmrkV2NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network.singular.SingularV2Api
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class RmrkV2Module {

    @Provides
    @FeatureScope
    fun provideSingularApi(networkApiCreator: NetworkApiCreator): SingularV2Api {
        return networkApiCreator.create(SingularV2Api::class.java, SingularV2Api.BASE_URL)
    }

    @Provides
    @FeatureScope
    fun provideNftProvider(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        singularV2Api: SingularV2Api,
        nftDao: NftDao,
        gson: Gson
    ) = RmrkV2NftProvider(chainRegistry, accountRepository, singularV2Api, nftDao, gson)
}
