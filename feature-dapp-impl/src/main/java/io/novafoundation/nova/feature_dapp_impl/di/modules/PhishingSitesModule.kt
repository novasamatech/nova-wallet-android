package io.novafoundation.nova.feature_dapp_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.PhishingSitesDao
import io.novafoundation.nova.feature_dapp_impl.data.network.phishing.PhishingSitesApi
import io.novafoundation.nova.feature_dapp_impl.data.phisning.BlackListPhishingDetectingService
import io.novafoundation.nova.feature_dapp_impl.data.phisning.CompoundPhishingDetectingService
import io.novafoundation.nova.feature_dapp_impl.data.phisning.DomainListPhishingDetectingService
import io.novafoundation.nova.feature_dapp_impl.data.phisning.PhishingDetectingService
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepositoryImpl

private val PHISHING_DOMAINS = listOf("top")

@Module
class PhishingSitesModule {

    @Provides
    @FeatureScope
    fun providePhishingDetectingService(
        phishingSitesDao: PhishingSitesDao
    ): PhishingDetectingService {
        return CompoundPhishingDetectingService(
            listOf(
                BlackListPhishingDetectingService(phishingSitesDao),
                DomainListPhishingDetectingService(PHISHING_DOMAINS)
            )
        )
    }

    @Provides
    @FeatureScope
    fun providePhishingSitesApi(networkApiCreator: NetworkApiCreator): PhishingSitesApi {
        return networkApiCreator.create(PhishingSitesApi::class.java)
    }

    @Provides
    @FeatureScope
    fun providePhishingSitesRepository(
        api: PhishingSitesApi,
        phishingSitesDao: PhishingSitesDao,
        phishingDetectingService: PhishingDetectingService
    ): PhishingSitesRepository = PhishingSitesRepositoryImpl(phishingSitesDao, api, phishingDetectingService)
}
