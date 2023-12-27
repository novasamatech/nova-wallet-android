package io.novafoundation.nova.feature_nft_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.repository.NftRepositoryImpl
import io.novafoundation.nova.feature_nft_impl.data.source.JobOrchestrator
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvidersRegistry
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.RmrkV1NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.RmrkV2NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.UniquesNftProvider
import io.novafoundation.nova.feature_nft_impl.di.modules.RmrkV1Module
import io.novafoundation.nova.feature_nft_impl.di.modules.RmrkV2Module
import io.novafoundation.nova.feature_nft_impl.di.modules.UniquesModule
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(
    includes = [
        UniquesModule::class,
        RmrkV1Module::class,
        RmrkV2Module::class
    ]
)
class NftFeatureModule {

    @Provides
    @FeatureScope
    fun provideJobOrchestrator() = JobOrchestrator()

    @Provides
    @FeatureScope
    fun provideNftProviderRegistry(
        uniquesNftProvider: UniquesNftProvider,
        rmrkV1NftProvider: RmrkV1NftProvider,
        rmrkV2NftProvider: RmrkV2NftProvider,
    ) = NftProvidersRegistry(uniquesNftProvider, rmrkV1NftProvider, rmrkV2NftProvider)

    @Provides
    @FeatureScope
    fun provideNftRepository(
        nftProvidersRegistry: NftProvidersRegistry,
        chainRegistry: ChainRegistry,
        jobOrchestrator: JobOrchestrator,
        nftDao: NftDao,
        httpExceptionHandler: HttpExceptionHandler,
    ): NftRepository = NftRepositoryImpl(
        nftProvidersRegistry = nftProvidersRegistry,
        chainRegistry = chainRegistry,
        jobOrchestrator = jobOrchestrator,
        nftDao = nftDao,
        exceptionHandler = httpExceptionHandler
    )
}
