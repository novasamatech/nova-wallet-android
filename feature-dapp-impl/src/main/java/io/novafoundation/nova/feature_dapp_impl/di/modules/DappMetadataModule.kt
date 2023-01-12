package io.novafoundation.nova.feature_dapp_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.BrowserHostSettingsDao
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.BuildConfig
import io.novafoundation.nova.feature_dapp_impl.data.network.metadata.DappMetadataApi
import io.novafoundation.nova.feature_dapp_impl.data.repository.RealDAppMetadataRepository

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
        api: DappMetadataApi,
        dappHostSettingsDao: BrowserHostSettingsDao
    ): DAppMetadataRepository = RealDAppMetadataRepository(
        dappMetadataApi = api,
        remoteApiUrl = BuildConfig.DAPP_METADATAS_URL,
        browserHostSettingsDao = dappHostSettingsDao
    )
}
