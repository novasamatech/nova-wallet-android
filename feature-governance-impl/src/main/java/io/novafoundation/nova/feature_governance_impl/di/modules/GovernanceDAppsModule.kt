package io.novafoundation.nova.feature_governance_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_impl.data.dapps.GovernanceDAppsSyncService
import io.novafoundation.nova.feature_governance_impl.data.dapps.remote.GovernanceDappsFetcher
import io.novafoundation.nova.feature_governance_impl.data.repository.RealGovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_impl.domain.dapp.GovernanceDAppsInteractor

@Module
class GovernanceDAppsModule {

    @Provides
    @FeatureScope
    fun provideGovernanceDappsFetcher(apiCreator: NetworkApiCreator) = apiCreator.create(GovernanceDappsFetcher::class.java)

    @Provides
    @FeatureScope
    fun provideGovernanceSyncService(
        dao: GovernanceDAppsDao,
        chainFetcher: GovernanceDappsFetcher
    ) = GovernanceDAppsSyncService(dao, chainFetcher)

    @Provides
    @FeatureScope
    fun provideDAppsRepository(governanceDAppsDao: GovernanceDAppsDao): GovernanceDAppsRepository {
        return RealGovernanceDAppsRepository(governanceDAppsDao)
    }

    @Provides
    @FeatureScope
    fun provideGovernanceDAppInteractor(
        governanceDAppsSyncService: GovernanceDAppsSyncService,
        governanceDAppsRepository: GovernanceDAppsRepository
    ): GovernanceDAppsInteractor = GovernanceDAppsInteractor(
        governanceDAppsSyncService = governanceDAppsSyncService,
        governanceDAppsRepository = governanceDAppsRepository
    )
}
