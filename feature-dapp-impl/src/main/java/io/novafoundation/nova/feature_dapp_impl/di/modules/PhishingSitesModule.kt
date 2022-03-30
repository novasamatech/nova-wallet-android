package io.novafoundation.nova.feature_dapp_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.PhishingSitesDao
import io.novafoundation.nova.feature_dapp_impl.data.network.phishing.PhishingSitesApi
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepositoryImpl

@Module
class PhishingSitesModule {

    @Provides
    @FeatureScope
    fun providePhishingSitesApi(networkApiCreator: NetworkApiCreator): PhishingSitesApi {
        return networkApiCreator.create(PhishingSitesApi::class.java)
    }

    @Provides
    @FeatureScope
    fun providePhishingSitesRepository(
        api: PhishingSitesApi,
        phishingSitesDao: PhishingSitesDao
    ): PhishingSitesRepository = PhishingSitesRepositoryImpl(phishingSitesDao, api)
}
