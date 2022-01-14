package io.novafoundation.nova.feature_dapp_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.BuildConfig
import io.novafoundation.nova.feature_dapp_impl.data.network.api.DappMetadataApi
import io.novafoundation.nova.feature_dapp_impl.data.repository.InMemoryDAppMetadataRepository

@Module
class DappMetadataModule {

    @Provides
    @FeatureScope
    fun provideApi(
        apiCreator: NetworkApiCreator
    ) = apiCreator.create(DappMetadataApi::class.java)

    @Provides
    @FeatureScope
    fun provideDRepository(
        api: DappMetadataApi
    ): DAppMetadataRepository = InMemoryDAppMetadataRepository(
        dappMetadataApi = api,
        remoteApiUrl = BuildConfig.DAPP_METADATAS_URL
    )
}
